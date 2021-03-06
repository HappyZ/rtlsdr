/*
 * rtl-sdr, turns your Realtek RTL2832 based DVB dongle into a SDR receiver
 * Copyright (C) 2012 by Steve Markgraf <steve@steve-m.de>
 * Copyright (C) 2012 by Hoernchen <la@tfc-server.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


#include <errno.h>
#include <signal.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>

#ifndef _WIN32
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <fcntl.h>
#else
#include <WinSock2.h>
#include "getopt/getopt.h"
#endif

#include <pthread.h>

#include "rtl-sdr.h"

#include "log.h"

static struct logger log = {
	.name = "rtl_tcp",
	.log_level = LEVEL_TRACE,
	.log_func = default_log,
};

#ifdef _WIN32
#pragma comment(lib, "ws2_32.lib")

typedef int socklen_t;

#else
#define closesocket close
#define SOCKADDR struct sockaddr
#define SOCKET int
#define SOCKET_ERROR -1
#endif

static SOCKET s;

static pthread_t tcp_worker_thread;
static pthread_t command_thread;
static pthread_cond_t exit_cond;
static pthread_mutex_t exit_cond_lock;
static volatile int dead[2] = {0, 0};

static pthread_mutex_t ll_mutex;
static pthread_cond_t cond;

struct llist {
	char *data;
	size_t len;
	struct llist *next;
};

static rtlsdr_dev_t *dev = NULL;

int global_numq = 0;
static struct llist *ll_buffers = 0;

static int do_exit = 0;

void usage(void)
{
	log_info(&log,
		"rtl_tcp, an I/Q spectrum server for RTL2832 based DVB-T receivers\n\n"
		"Usage:\t[-a listen address]\n"
		"\t[-p listen port (default: 1234)]\n"
		"\t[-f frequency to tune to [Hz]]\n"
		"\t[-g gain (default: 0 for auto)]\n"
		"\t[-s samplerate in Hz (default: 2048000 Hz)]\n"
		"\t[-b number of buffers (default: 32, set by library)]\n"
		"\t[-d device index (default: 0)]\n");
	exit(1);
}

#ifdef _WIN32
int gettimeofday(struct timeval *tv, void* ignored)
{
	FILETIME ft;
	unsigned __int64 tmp = 0;
	if (NULL != tv) {
		GetSystemTimeAsFileTime(&ft);
		tmp |= ft.dwHighDateTime;
		tmp <<= 32;
		tmp |= ft.dwLowDateTime;
		tmp /= 10;
		tmp -= 11644473600000000Ui64;
		tv->tv_sec = (long)(tmp / 1000000UL);
		tv->tv_usec = (long)(tmp % 1000000UL);
	}
	return 0;
}

BOOL WINAPI
sighandler(int signum)
{
	if (CTRL_C_EVENT == signum) {
		log_warn(&log, "Signal caught, exiting!\n");
		do_exit = 1;
		rtlsdr_cancel_async(dev);
		return TRUE;
	}
	return FALSE;
}
#else
static void sighandler(int signum)
{
	log_warn(&log, "Signal caught, exiting!\n");
	do_exit = 1;
	rtlsdr_cancel_async(dev);
}
#endif

void rtlsdr_callback(unsigned char *buf, uint32_t len, void *ctx)
{
    //Tests by Ana
    //char str[11]; /* 11 bytes: 10 for the digits, 1 for the null character */
    //snprintf(str, sizeof str, "%" PRIu32, len);
    //log_info(&log, "rtlsdr_callback len %s\n", str);

	if(!do_exit) {
		struct llist *rpt = (struct llist*)malloc(sizeof(struct llist));
		rpt->data = (char*)malloc(len);
		memcpy(rpt->data, buf, len);
		rpt->len = len;
		rpt->next = NULL;

		pthread_mutex_lock(&ll_mutex);

		if (ll_buffers == NULL) {
			ll_buffers = rpt;
		} else {
			struct llist *cur = ll_buffers;
			int num_queued = 0;

			while (cur->next != NULL) {
				cur = cur->next;
				num_queued++;
			}
			cur->next = rpt;

			if (num_queued > global_numq)
				log_info(&log,"ll+, now %d\n", num_queued);
			else if (num_queued < global_numq)
				log_info(&log, "ll-, now %d\n", num_queued);

			global_numq = num_queued;
		}
		pthread_cond_signal(&cond);
		pthread_mutex_unlock(&ll_mutex);
	}
}

static void *tcp_worker(void *arg)
{
	struct llist *curelem,*prev;
	int bytesleft,bytessent, index;
	struct timeval tv= {1,0};
	struct timespec ts;
	struct timeval tp;
	fd_set writefds;
	int r = 0;

	while(1) {
		if(do_exit)
		{
		    sighandler(0);
            dead[1]=1;
            pthread_exit(NULL);
		}

		pthread_mutex_lock(&ll_mutex);
		gettimeofday(&tp, NULL);
		ts.tv_sec  = tp.tv_sec+1;
		//ts.tv_sec  = tp.tv_sec+5; //changed by Ana version 1
		ts.tv_nsec = tp.tv_usec * 1000;
		r = pthread_cond_timedwait(&cond, &ll_mutex, &ts);
		if(r == ETIMEDOUT) {
			pthread_mutex_unlock(&ll_mutex);
			log_warn(&log,"worker cond timeout\n");
			sighandler(0);
			dead[1]=1;
			pthread_exit(NULL);
		}

		curelem = ll_buffers;
		ll_buffers = 0;
		pthread_mutex_unlock(&ll_mutex);

		while(curelem != 0) {
			bytesleft = curelem->len;
			//log_info(&log, "bytes left %d\n", bytesleft);
			index = 0;
			bytessent = 0;
			while(bytesleft > 0) {
				FD_ZERO(&writefds);
				FD_SET(s, &writefds);
				tv.tv_sec = 1;
				tv.tv_usec = 0;
				r = select(s+1, NULL, &writefds, NULL, &tv);
				if(r) {
					bytessent = send(s,  &curelem->data[index], bytesleft, 0);
					if (bytessent == SOCKET_ERROR || do_exit) {
						log_warn(&log, "worker socket error\n");
						sighandler(0);
						dead[1]=1;
						pthread_exit(NULL);
					} else {
						bytesleft -= bytessent;
						index += bytessent;
					}
				} else if(do_exit) {
						log_debug(&log, "worker socket bye\n");
						sighandler(0);
						dead[1]=1;
						pthread_exit(NULL);
				}
			}
			prev = curelem;
			curelem = curelem->next;
			free(prev->data);
			free(prev);
		}
	}
}

#ifdef _WIN32
#define __attribute__(x)
#pragma pack(push, 1)
#endif
struct command{
	unsigned char cmd;
	unsigned int param;
	unsigned int param1; //added by Ana
	unsigned int param2; //added by Ana
}__attribute__((packed));
#ifdef _WIN32
#pragma pack(pop)
#endif
static void *command_worker(void *arg)
{
	int left, received;
	fd_set readfds;
	struct command cmd={0, 0, 0, 0}; //changed by Ana
	struct timeval tv= {1, 0};
	int r =0;

	//log_info(&log, "sizeof cmd %d\n", sizeof(cmd));
	while(1) {
		//left=sizeof(cmd);
		//while(left >0) {
			FD_ZERO(&readfds);
			FD_SET(s, &readfds);
			tv.tv_sec = 1;
			tv.tv_usec = 0;
			r = select(s+1, &readfds, NULL, NULL, &tv);
			if(r) {
			    //changed by Ana
				//received = recv(s, (char*)&cmd+(sizeof(cmd)-left), left, 0);
				received = recv(s, (char*)&cmd, sizeof(cmd), 0);
				if(received == SOCKET_ERROR || do_exit){
					log_warn(&log, "comm recv socket error\n");
					sighandler(0);
					dead[0]=1;
					pthread_exit(NULL);
				}
				//else {
				    //log_info(&log, "received %d\n", ntohl(received));
					//left -= received;

				//}
			} else if(do_exit) {
				log_debug(&log, "comm recv bye\n");
				sighandler(0);
				dead[0] = 1;
				pthread_exit(NULL);
			}
		//}
		switch(cmd.cmd) {
		case 0x01:
			log_info(&log, "set freq %d\n", ntohl(cmd.param));
			rtlsdr_set_center_freq(dev,ntohl(cmd.param));
			rtlsdr_set_sample_rate(dev, ntohl(cmd.param1)); //changed by Ana
			rtlsdr_set_tuner_gain(dev, ntohl(cmd.param2)); //changed by Ana
			log_info(&log, "set sample rate %d\n", ntohl(cmd.param1));
			log_info(&log, "set tuner gain %f\n", ntohl(cmd.param2)/10.0);
			break;
		case 0x02:
			log_info(&log, "set sample rate %d\n", ntohl(cmd.param));
			rtlsdr_set_sample_rate(dev, ntohl(cmd.param));
			break;
		case 0x03:
			log_info(&log, "set gain mode %d\n", ntohl(cmd.param));
			rtlsdr_set_tuner_gain_mode(dev, ntohl(cmd.param));
			break;
		case 0x04:
			log_info(&log, "set gain %d\n", ntohl(cmd.param));
			rtlsdr_set_tuner_gain(dev, ntohl(cmd.param));
			break;
		case 0x05:
			log_info(&log, "set freq correction %d\n", ntohl(cmd.param));
			rtlsdr_set_freq_correction(dev, ntohl(cmd.param));
			break;
		case 0x06:
		    log_info(&log, "set end %d\n", ntohl(cmd.param));
		    sighandler(0);
        	dead[0]=1;
        	pthread_exit(NULL);
		    break;
		default:
			break;
		}
		cmd.cmd = 0xff;
	}
}

int main(int argc, char **argv)
{
	log_debug(&log, "SUP");
	int r, opt, i;
	char* addr = "127.0.0.1";
	int port = 1234;
	uint32_t frequency = 100000000, samp_rate = 2048000;
	struct sockaddr_in local, remote;
	int device_count;
	uint32_t dev_index = 0, buf_num = 0;
	//int gain = 0;
	//changed by Ana
	int gain = 420;
	//*******
	struct llist *curelem,*prev;
	pthread_attr_t attr;
	void *status;
	struct timeval tv = {1,0};
	struct linger ling = {1,0};
	SOCKET listensocket;
	socklen_t rlen;
	fd_set readfds;
	u_long blockmode = 1;
#ifdef _WIN32
	WSADATA wsd;
	i = WSAStartup(MAKEWORD(2,2), &wsd);
#else
	struct sigaction sigact, sigign;
#endif

	while ((opt = getopt(argc, argv, "a:p:f:g:s:b:d:")) != -1) {
		switch (opt) {
		case 'd':
			dev_index = atoi(optarg);
			break;
		case 'f':
			frequency = (uint32_t)atof(optarg);
			break;
		case 'g':
			gain = (int)(atof(optarg) * 10); /* tenths of a dB */
			break;
		case 's':
			samp_rate = (uint32_t)atof(optarg);
			break;
		case 'a':
			addr = optarg;
			break;
		case 'p':
			port = atoi(optarg);
			break;
		case 'b':
			buf_num = atoi(optarg);
			break;
		default:
			usage();
			break;
		}
	}

	if (argc < optind)
		usage();

	device_count = rtlsdr_get_device_count();
	if (!device_count) {
		log_warn(&log, "No supported devices found.\n");
		return EXIT_FAILURE;
	}

	log_info(&log, "Found %d device(s).\n", device_count);

	rtlsdr_open(&dev, dev_index);
	if (NULL == dev) {
	log_warn(&log, "Failed to open rtlsdr device #%d.\n", dev_index);
		exit(1);
	}

	log_info(&log, "Using %s\n", rtlsdr_get_device_name(dev_index));
#ifndef _WIN32
	sigact.sa_handler = sighandler;
	sigemptyset(&sigact.sa_mask);
	sigact.sa_flags = 0;
	sigign.sa_handler = SIG_IGN;
	sigaction(SIGINT, &sigact, NULL);
	sigaction(SIGTERM, &sigact, NULL);
	sigaction(SIGQUIT, &sigact, NULL);
	sigaction(SIGPIPE, &sigign, NULL);
#else
	SetConsoleCtrlHandler( (PHANDLER_ROUTINE) sighandler, TRUE );
#endif
	/* Set the sample rate */
	r = rtlsdr_set_sample_rate(dev, samp_rate);
	if (r < 0)
		log_warn(&log, "WARNING: Failed to set sample rate.\n");

	/* Set the frequency */
	r = rtlsdr_set_center_freq(dev, frequency);
	if (r < 0)
		log_warn(&log, "WARNING: Failed to set center freq.\n");
	else
		log_warn(&log, "Tuned to %i Hz.\n", frequency);

	if (0 == gain) {
		 /* Enable automatic gain */
		r = rtlsdr_set_tuner_gain_mode(dev, 0);
		if (r < 0)
			log_warn(&log, "WARNING: Failed to enable automatic gain.\n");
	} else {
		/* Enable manual gain */
		r = rtlsdr_set_tuner_gain_mode(dev, 1);
		if (r < 0)
			log_warn(&log, "WARNING: Failed to enable manual gain.\n");

		/* Set the tuner gain */
		r = rtlsdr_set_tuner_gain(dev, gain);
		if (r < 0)
			log_warn(&log, "WARNING: Failed to set tuner gain.\n");
		else
			log_warn(&log, "Tuner gain set to %f dB.\n", gain/10.0);
	}

	/* Reset endpoint before we start reading from it (mandatory) */
	r = rtlsdr_reset_buffer(dev);
	if (r < 0)
		log_warn(&log, "WARNING: Failed to reset buffers.\n");

	pthread_mutex_init(&exit_cond_lock, NULL);
	pthread_mutex_init(&ll_mutex, NULL);
	pthread_mutex_init(&exit_cond_lock, NULL);
	pthread_cond_init(&cond, NULL);
	pthread_cond_init(&exit_cond, NULL);

	memset(&local,0,sizeof(local));
	local.sin_family = AF_INET;
	local.sin_port = htons(port);
	local.sin_addr.s_addr = inet_addr(addr);

	listensocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	r = 1;
	setsockopt(listensocket, SOL_SOCKET, SO_REUSEADDR, (char *)&r, sizeof(int));
	setsockopt(listensocket, SOL_SOCKET, SO_LINGER, (char *)&ling, sizeof(ling));
	bind(listensocket,(struct sockaddr *)&local,sizeof(local));

	#ifdef _WIN32
	ioctlsocket(listensocket, FIONBIO, &blockmode);
	#else
	r = fcntl(listensocket, F_GETFL, 0);
	r = fcntl(listensocket, F_SETFL, r | O_NONBLOCK);
	#endif

	while(1) {
		log_info(&log, "listening...\n");
		log_info(&log, 
			"Use the device argument 'rtl_tcp=%s:%d' in OsmoSDR "
		       "(gr-osmosdr) source\n"
		       "to receive samples in GRC and control "
		       "rtl_tcp parameters (frequency, gain, ...).\n",
		       addr, port);
		listen(listensocket,1);

		while(1) {
			FD_ZERO(&readfds);
			FD_SET(listensocket, &readfds);
			tv.tv_sec = 1;
			tv.tv_usec = 0;
			r = select(listensocket+1, &readfds, NULL, NULL, &tv);
			if(do_exit) {
				goto out;
			} else if(r) {
				rlen = sizeof(remote);
				s = accept(listensocket,(struct sockaddr *)&remote, &rlen);
				break;
			}
		}

		setsockopt(s, SOL_SOCKET, SO_LINGER, (char *)&ling, sizeof(ling));

		log_debug(&log, "client accepted!\n");

		pthread_attr_init(&attr);
		pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
		r = pthread_create(&command_thread, &attr, command_worker, NULL);
		sleep(0.5);
		r = pthread_create(&tcp_worker_thread, &attr, tcp_worker, NULL);
		pthread_attr_destroy(&attr);

		r = rtlsdr_read_async(dev, rtlsdr_callback, (void *)0,
				      buf_num, 0);


		closesocket(s);
		if(!dead[0])
		    pthread_join(command_thread, &status);

		if(!dead[1])
			pthread_join(tcp_worker_thread, &status);


		log_debug(&log, "all threads dead..\n");
		curelem = ll_buffers;
		ll_buffers = 0;

		while(curelem != 0) {
			prev = curelem;
			curelem = curelem->next;
			free(prev->data);
			free(prev);
		}

		do_exit = 0;
		global_numq = 0;
		dead[0]=0;
		dead[1]=0;
	}

out:
	rtlsdr_close(dev);
	closesocket(listensocket);
	closesocket(s);
	#ifdef _WIN32
	WSACleanup();
	#endif
	log_info(&log, "bye!\n");
	return r >= 0 ? r : -r;
}
#ifdef ANDROID


#include <jni.h>
#include "libusbhelper.h"
JNIEXPORT jint JNICALL Java_rtlsdr_android_MainActivity_nativeRtlSdrTcp(JNIEnv *envp, jobject objp)
{
  log_info(&log, "Starting native tcp\n");
  init_libusbhelper(envp,objp);
//  char * args[] = { "rtl_tcp" , "-a" , "0.0.0.0" , "-p" , "1234"};
	char * args[] = {"rtl_tcp","-p","5001"};
  return main(3,args);
}

#endif

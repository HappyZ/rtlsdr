# DO NOT EDIT
# This makefile makes sure all linkable targets are
# up-to-date with anything they link to
default:
	echo "Do not invoke directly"

# For each target create a dummy rule so the target does not have to exist
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/librtlsdr.dylib:
/opt/local/lib/libusb-1.0.dylib:
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/librtlsdr.dylib:
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/librtlsdr.dylib:
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/librtlsdr.dylib:


# Rules to remove targets that are older than anything to which they
# link.  This forces Xcode to relink the targets from scratch.  It
# does not seem to check these dependencies itself.
PostBuild.rtl_fm.Debug:
PostBuild.rtlsdr_shared.Debug: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_fm
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_fm:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_fm


PostBuild.rtl_sdr.Debug:
PostBuild.rtlsdr_shared.Debug: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_sdr
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_sdr:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_sdr


PostBuild.rtl_tcp.Debug:
PostBuild.rtlsdr_shared.Debug: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_tcp
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_tcp:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_tcp


PostBuild.rtl_test.Debug:
PostBuild.rtlsdr_shared.Debug: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_test
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_test:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/rtl_test


PostBuild.rtlsdr_shared.Debug:
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/librtlsdr.dylib:\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Debug/librtlsdr.dylib


PostBuild.rtlsdr_static.Debug:
PostBuild.rtl_fm.Release:
PostBuild.rtlsdr_shared.Release: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_fm
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_fm:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_fm


PostBuild.rtl_sdr.Release:
PostBuild.rtlsdr_shared.Release: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_sdr
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_sdr:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_sdr


PostBuild.rtl_tcp.Release:
PostBuild.rtlsdr_shared.Release: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_tcp
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_tcp:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_tcp


PostBuild.rtl_test.Release:
PostBuild.rtlsdr_shared.Release: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_test
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_test:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/rtl_test


PostBuild.rtlsdr_shared.Release:
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/librtlsdr.dylib:\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/Release/librtlsdr.dylib


PostBuild.rtlsdr_static.Release:
PostBuild.rtl_fm.MinSizeRel:
PostBuild.rtlsdr_shared.MinSizeRel: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_fm
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_fm:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_fm


PostBuild.rtl_sdr.MinSizeRel:
PostBuild.rtlsdr_shared.MinSizeRel: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_sdr
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_sdr:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_sdr


PostBuild.rtl_tcp.MinSizeRel:
PostBuild.rtlsdr_shared.MinSizeRel: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_tcp
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_tcp:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_tcp


PostBuild.rtl_test.MinSizeRel:
PostBuild.rtlsdr_shared.MinSizeRel: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_test
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_test:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/rtl_test


PostBuild.rtlsdr_shared.MinSizeRel:
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/librtlsdr.dylib:\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/MinSizeRel/librtlsdr.dylib


PostBuild.rtlsdr_static.MinSizeRel:
PostBuild.rtl_fm.RelWithDebInfo:
PostBuild.rtlsdr_shared.RelWithDebInfo: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_fm
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_fm:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_fm


PostBuild.rtl_sdr.RelWithDebInfo:
PostBuild.rtlsdr_shared.RelWithDebInfo: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_sdr
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_sdr:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_sdr


PostBuild.rtl_tcp.RelWithDebInfo:
PostBuild.rtlsdr_shared.RelWithDebInfo: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_tcp
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_tcp:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_tcp


PostBuild.rtl_test.RelWithDebInfo:
PostBuild.rtlsdr_shared.RelWithDebInfo: /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_test
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_test:\
	/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/librtlsdr.dylib\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/rtl_test


PostBuild.rtlsdr_shared.RelWithDebInfo:
/Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/librtlsdr.dylib:\
	/opt/local/lib/libusb-1.0.dylib
	/bin/rm -f /Users/sciencectn/Documents/CURRENT_Lab/android-rtlsdr-as/app/jni/src/RelWithDebInfo/librtlsdr.dylib


PostBuild.rtlsdr_static.RelWithDebInfo:

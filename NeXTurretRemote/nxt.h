/*
 * nxt.h
 *
 *  Created on: 13 nov. 2016
 *      Author: tchamelot
 */

#ifndef NXT_H_
#define NXT_H_

#include <iostream>
#include <string>
#include <cstdlib>
#include <cstring>
#include <libusb-1.0/libusb.h>

using namespace std;

class NXT {
public:
	NXT(bool debug = false);
	bool find_nxt();
	int open();
	int connect_ecrobot();
	int write(unsigned char* data, int size);
	int read(unsigned char* data, int size);
	int mailbox_send(string msg, unsigned char num);
	int mailbox_read(string &msg, unsigned char num);
	string get_name();
	virtual ~NXT();

protected:
	static bool isNXT(libusb_device* device);
	static libusb_context* NXT_CTX;
	static int NXT_CNT;
	static const unsigned char NXT_ENDPOINT_IN;
	static const unsigned char NXT_ENDPOINT_OUT;
	static const int ID_VENDOR;
	static const int ID_PRODUCT;
private:
	bool m_debug;

	libusb_device** m_devices;
	ssize_t m_dev_cnt;
	libusb_device* m_nxt;
	libusb_device_handle* m_handle;

};

#endif /* NXT_H_ */

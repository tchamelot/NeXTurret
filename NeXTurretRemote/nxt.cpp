/*
 * nxt.cpp
 *
 *  Created on: 13 nov. 2016
 *      Author: tchamelot
 */

#include "nxt.h"

libusb_context* NXT::NXT_CTX = NULL;
int NXT::NXT_CNT = 0;
const int NXT::ID_VENDOR 	= 0x0694;
const int NXT::ID_PRODUCT 	= 0X0002;
const unsigned char NXT::NXT_ENDPOINT_OUT = 0x01;
const unsigned char NXT::NXT_ENDPOINT_IN = 0x82;

NXT::NXT(bool debug)
{
	if(NXT_CNT == 0)
	{
		int err = libusb_init(&NXT_CTX);
		if(err < 0)
		{
			cerr << "Error while initializing LIBUSB-1.0" << endl;
		}
		if(debug)
			libusb_set_debug(NXT_CTX, 4);
	}
	NXT_CNT++;

	m_dev_cnt = libusb_get_device_list(NXT_CTX, &m_devices);
	if(m_dev_cnt < 0)
	{
		cerr << "Error while listing usb devices" << endl;
		exit(EXIT_FAILURE);
	}

	m_nxt = NULL;
	m_handle = NULL;
	m_debug = debug;
}

bool NXT::find_nxt()
{
	bool found;
	int idx;

	if(m_debug)
		cout << "Looking for NXT ..." << endl;

	found = false;

	for(idx = 0; idx < m_dev_cnt; idx++)
	{
		if(isNXT(m_devices[idx]) == true)
		{
			m_nxt = libusb_ref_device(m_devices[idx]);
			found = true;
			break;
		}
	}
	if(m_debug)
		if(found == true)
			cout << "NXT found" << endl;
		else
			cerr << "NXT not found" << endl;

	return found;
}

int NXT::open()
{
	int err;

	if(m_debug)
		cout << "Connecting to NXT ..." << endl;

	err = libusb_open(m_nxt, &m_handle);
	if(err == 0)
	{
		err = libusb_claim_interface(m_handle, 0);

		if(m_debug)
			if(err == 0)
				cout << "NXT Connected" << endl;
			else
				cerr << "Can't claim interface" << endl;
	}
	else
		if(m_debug)
			cerr << "Can't connect to NXT" << endl;

	return err;
}

bool NXT::isNXT(libusb_device* device)
{
	libusb_device_descriptor desc;
	bool test;

	libusb_get_device_descriptor(device, &desc);
	if(desc.idProduct == ID_PRODUCT && desc.idVendor == ID_VENDOR)
		test = true;
	else
		test = false;

	return test;
}

int NXT::write(unsigned char* data, int size)
{
	int err;
	int writed;

	if(m_debug)
		cout << "Writing to NXT" << endl;

	err = libusb_bulk_transfer(m_handle, NXT_ENDPOINT_OUT,
			data, size, &writed, 100);

	if(err != 0 && writed != size && m_debug)
		cerr << "Error while writing" << endl;

	return err;
}

int NXT::read(unsigned char* data, int size)
{
	int err;
	int read;

	if(m_debug)
		cout << "Reading from NXT" << endl;

	err = libusb_bulk_transfer(m_handle, NXT_ENDPOINT_IN,
			data, size , &read, 100);

	if(m_debug && err != 0)
		cerr << "Error while reading" << endl;

	return err;
}

int NXT::mailbox_send(string msg, unsigned char num)
{
	int length = msg.length();
	unsigned char buffer_out[length + 5];
	int err;

	*(buffer_out + 0) = 0x80;
	*(buffer_out + 1) = 0x09;
	*(buffer_out + 2) = num;
	*(buffer_out + 3) = length + 1;
	strcpy(reinterpret_cast<char*>(buffer_out + 4), msg.c_str());
	*(buffer_out + length + 4) = 0x00;

	err =  this->write(buffer_out, length + 5);

	return err;
}

int NXT::mailbox_read(string& msg, unsigned char num)
{
	unsigned char cmd[] = {0x00, 0x13, 0x00, 0x00, 1};
	unsigned char reply[64];
	int err;

	cmd[2] = num;
	cmd[3] = num;

	err = this->write(cmd, 5);
	if(err == 0)
	{
		err = this->read(reply, 64);
		if(reply[4] != 0)
		{
			string temp(reinterpret_cast<char*>(reply + 5));
			msg = temp;
		}
		else
			err = -1;
	}

	return err;
}

string NXT::get_name()
{
	unsigned char command[] = {0x01, 0x9B};
	unsigned char answer[33];
	string name;

	this->write(command, 2);
	this->read(answer, 33);

	name = reinterpret_cast<char*>(&answer[3]);

	return name;
}

NXT::~NXT()
{
	if(m_debug)
		cout << "Closing connection" << endl;

	if(m_handle != NULL)
	{
		libusb_release_interface(m_handle, 0);
		libusb_close(m_handle);
	}

	libusb_unref_device(m_nxt);
	libusb_free_device_list(m_devices, 1);

	NXT_CNT--;
	if(NXT_CNT == 0)
		libusb_exit(NXT_CTX);

	if(m_debug)
		cout << "Conenction closed" << endl;
}

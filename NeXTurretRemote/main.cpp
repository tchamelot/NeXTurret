/**
 * Project: NeXTurretRemote
 * File: main.cpp
 * Autor: T. Chamelot
 * Date: 11/29/2016
 */

#include <iostream>
#include <cstdio>
#include <cstdlib>
#include <sys/ioctl.h>
#include <string>
#include <termios.h>
#include "nxt.h"

using namespace std;

int keypressed()
{
	static const int STDIN = 0;
	static bool initialized = false;
	termios term;
	int bytesWaiting;

	if(!initialized)
	{
		tcgetattr(STDIN, &term);
		term.c_lflag &= ~ICANON;
		tcsetattr(STDIN, TCSANOW, &term);
		setbuf(stdin, NULL);

		cin.sync_with_stdio();

		initialized = true;
	}

	ioctl(STDIN, FIONREAD, &bytesWaiting);
	return bytesWaiting;
}

void clear()
{
	cout << "\x1b[2J\x1b[0;0f" << flush;
}

void gotoxy(int x, int y)
{
	cout << "\x1b[" << y << ';' << x << 'f' <<  flush;
}

int main(int argc, char* argv[])
{
	NXT nxt(false);
	char cmd[3];
	char c;
	char v = 0;	//Vertical target position
	char h = 0;	//Horizontal target position
	//string cmd;
	string hk; 	//Housekeeping
	char raw_hk[4];
	
	//Clear screen
	clear();

	//Ensure that the turret is connected
	if(!nxt.find_nxt())
	{
		cerr << "Turret not connected" << endl;
		cerr << "Leaving NeXTurret Remote" << endl;
		exit(EXIT_FAILURE);
	}	
	else
	{
		nxt.open();
	}

	//Background task
	do
	{
		if(keypressed())
		{
			c = getchar();
			//directionnal arrow
			if(c == '\x1b')
			{
				c = getchar();
				c = getchar();

				switch(c)
				{
					//UP
					case 'A':
						v+=2;
						if(v > 100)
							v = 100;
						break;
					//DOWN
					case 'B':
						v-=2;
						if(v < -100)
							v = -100;
						break;
					//RIGHT
					case 'C':
						h+=2;
						if(h > 100)
							h = 100;
						break;
					//LEFT
					case 'D':
						h-=2;
						if(h < -100)
							h = -100;
						break;
				}

				if(v == 0)
					v = 1;
				if(h == 0)
					h = 1;


				cmd[0] = h;
				cmd[1] = v;
				cmd[2] = 0;
				nxt.mailbox_send(string(cmd), 0);
			}
		}

		if(nxt.mailbox_read(hk, 1) == 0)
		{
			strncpy(raw_hk, hk.c_str(), 4);
			gotoxy(0, 0);
			cout << "Raw angles : " << (int)raw_hk[3] << " " << (int)raw_hk[2] << endl;
			cout << "Bullets : " << (int)raw_hk[1] << endl;
			cout <<	"Battery : " << (int)raw_hk[0] << endl;
			cout << "h : " << (int)h << endl << "  v : " << (int)v << endl;
		}

	}while(c != 'Q');
}

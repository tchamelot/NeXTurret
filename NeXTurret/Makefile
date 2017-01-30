NXC = nbc
FLAGS = -EF

SRC = main.nxc

all: NeXTurret
	t2n -put *.rxe

NeXTurret:
	$(NXC) $(FLAGS) $(SRC) -O="$@.rxe"

clean:
	rm -rf *.rxe
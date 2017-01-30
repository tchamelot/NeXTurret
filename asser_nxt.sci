//Script Scilab pour correcteur PID pour moteur de NXT
clear
clc
xdel(winsid())

//Mesures expérimentales en boucles ouvertes
ymes = [0 4 8 13 19 25 32 39 46 54 62 70 77 85 93 101 109 117 125 133 141 149 157 165 173 182 190 198 206 214 222 230 238 246 254];
tmes = [0 0.02 0.04 0.06 0.08 0.1 0.12 0.14 0.16 0.18 0.2 0.22 0.24 0.26 0.28 0.3 0.32 0.34 0.36 0.38 0.4 0.42 0.44 0.46 0.48 0.5 0.52 0.54 0.56 0.58 0.6 0.62 0.64 0.66 0.68];

//Carractéristiques moteur validés pour le moment
Tm  = 0.045;         //[s]
Km  = 1.8;         //[(rad/s)/V]
Bat = 7.732 * 0.01;   //[V] /!\ A modifer par rapport à la valeur actuelle
Kip = 360/(2*%pi);  //[rad/s] -> [°/s]

//Carractéristique de simulation
ech = 90;
pente = 10;

//Cahier des charges
fn = 4;         //[Hz]
Pmarg = 60;     //[°]
//Errp = 0;
//Errv =0;
//Saturation : sortie correcteur < 90 pour une entrée de 10

//Fonctions de transfere
//Moteur
FT_mot  = syslin('c',Km*Bat, poly([0 1 Tm], 'p', 'c'))
//Chaine directe
FT_dir  = FT_mot;
//Boulce ouverte
FT_bo   = FT_dir * Kip;
//Boucle fermée
FT_bf   = FT_dir /(1 + FT_dir*Kip);

//Réponses temporelles
t = 0:0.001:3;
step = linspace(ech, ech, size(t, "*"));
//Comparaison modèle aux valeurs expérimentales 
ymod = csim(step*50/ech, t, FT_bo);
scf(1)
clf
plot(t, ymod, 'r');
plot(tmes, ymes, 'b');
xtitle("Comparaison des valeures expérimentales au modèle");
//Réponse indicielle sans correcteur
S = csim(step, t, FT_bf);


//Détermination du correcteur
FT_p = (2*%pi*fn)*(2*%pi*fn)*(Tm/(Km*Kip*Bat));
disp(FT_p, "Correcteur P : ");
//TODO prendre en compte la saturation
[Mphi, w1] = p_margin(FT_bo*FT_p);
disp(Mphi, "Marge de phase avec correcteur P : ");
phi_m = 1.5*(Pmarg - Mphi);
a = (1 + sin(phi_m))/(1 - sin(phi_m));
scf(2)
clf
bode(FT_bo*FT_p, 0.01, 100);
xtitle("Correcteur proportionnel")
disp(-10*log10(a), "Entrez la fréquence pour laquelle le gain est ");
w = input("Fréquence : ");
w = 2*%pi*w;
Ta = 1/(w*sqrt(a));
FT_ap = FT_p * syslin('c', poly([1 a*Ta], 'p', 'c'), poly([1 Ta], 'p', 'c'));
scf(3)
clf
bode(FT_bo*FT_ap, 0.01, 100);
xtitle("Correcteur à avance de phase");
disp(FT_ap, "Correcteur à avance de phase : ");
disp(Ta, " Tf = ", (a*Ta - Ta), "Td = ");
[Mphi, w1] = p_margin(FT_bo*FT_ap);
disp(Mphi, "Marge de phase avec correcteur à avance de phase : ");
disp(w1*2*%pi, "w1 correcteur à avance de phase : ");
Ti = tand((Pmarg - Mphi) - 90)/(w1*2*%pi);
FT_pid  = syslin('c', poly([1 Ti], 'p', 'c'), poly([0 Ti], 'p', 'c')) * FT_ap;
scf(4)
clf
bode(FT_bo*FT_pid, 0.01, 100);
xtitle("Correcteur PID");
disp(FT_pid, "Correcteur PID : ");
disp(Ti, "Ti = ");
[Mphi, w1] = p_margin(FT_bo*FT_pid);
disp(Mphi, "Marge de phase avec correcteur PID : ");

scf(5)
clf
bode([FT_p; FT_ap; FT_pid], 0.01, 100, ["P"; "AP"; "PID"]);
xtitle("Fonctions de transfère des correcteurs")

scf(6)
clf
bode([FT_p*FT_bo; FT_ap*FT_bo; FT_pid*FT_bo], 0.01, 100, ["P"; "AP"; "PID"]);

FT_bo = FT_dir*FT_pid*Kip;
FT_bf = 1/Kip * FT_bo/(1 + FT_bo);
disp(FT_pid, "Fonction de transfer en boucle fermée avec correcteur PID : ");

//Réponse temporelle
scf(7)
clf
Spid = csim(step, t, FT_bf);
plot(t, step);
plot(t, S*Kip, 'r');
plot(t, Spid*Kip, 'b');
xtitle("Réponse indicielle");

//Réponse fréquentielle
scf(8)
clf
bode(FT_bo, 0.01, 100);
xtitle("Réponse fréquencielle");

//Numérisation
w_tustin = 4; //Wn = (Kp*Km*Bat*Kip/Tm)^0.5 ~= 25[rad/s] <= fn = 4Hz | On choisit w_tustin = Wn. Comme cls2dls() prends une fréquence en paramètre on choisi w_tustin = fn
//Système
Pss = tf2ss(FT_dir);
Pdss = cls2dls(Pss, 0.01, w_tustin);
//Pdss = dscr(Pss, 0.01);
FTd_dir = ss2tf(Pdss);
//Correcteur
Css = tf2ss(FT_pid);
Cdss = cls2dls(Css, 0.01, w_tustin);
//Cdss = dscr(Css, 0.01);
FTd_pid = ss2tf(Cdss);
disp(FTd_pid, "Fonction de transfer numérique du correcteur PID")
scf(9)
clf
bode(FTd_pid, 0.01, 100)
xtitle("Correcteur PID numérique")
FTd_bo = FTd_dir * FTd_pid * Kip;
FTd_bf = 1/Kip * FTd_bo/(1 + FTd_bo)
scf(10)
clf
bode(FTd_bo, 0.01, 100)
xtitle("Comparaison numérique analogique");

//Réponse temporelle numérique
td = 0:0.01:3;
stepn = linspace(ech, ech, size(td, "*"));
Sn = dsimul(tf2ss(FTd_bf), stepn);
scf(11)
clf
plot(t, step);
plot(t, Spid*Kip, 'b');
plot2d2(td, Sn*Kip);
xtitle("Réponse indicielle numérique");

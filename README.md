# Süsteem füüsiliste klotsidega programmeerimiseks
Füüsiliste klotsidega programmeerimise süsteem
on mõeldud programmeerimise tutvustamiseks
ning selle õppimise lihtsustamiseks. Sellised
süsteemid võimaldavad programme koostada
käske kujutavate klotside rittaseadmise teel.
Klotsidega programmeerimiskeeled võimaldavad
minimeerida või täielikult kaotada programmist
süntaksivead ning selle läbi motiveerida inimesi
rohkem programmeerimist katsetama ja õppima.
Samuti on klotside puhul võimalike komponentide
loetelu ning võimalikud ühendamisviisid selgemini
näha, mis omakorda lihtsustab võhikul
programmeerimise õppimist.

## Projekti üles seadmine
Eraldi juhendid antakse Windowsi ning Linuxi keskkonna jaoks. 
Ühtlasi on kasutajal võimalik rakendust ehitada ka lähtekoodist, 
mille jaoks antakse samuti õpetus. Juhendites eeldatakse, 
et kasutajal on olemas Java 8 arenduskeskkond. Tähtis on, et arenduskeskkond sisaldaks ka JavaFX teeki.

### Juhend Windowsi jaoks
Projekti jooksutamiseks on vajalik OpenCV teek versiooniga 3.2.0, mille saab alla laadida OpenCV
koduleheküljelt. Sealt saab alla laadida .exe vormingus faili (Win pack), 
mida jooksutades installitakse teek.
Projekti juurkataloogis on eelnevalt kompileeritud rakenduse kokkupakitud .jar fail. 
Jooksutamiseks on vaja avada käsuviip (command prompt) ning navigeerida projekti juurkataloogi. 
Seejärel saab järgmise käsuga rakenduse käivitada:

java -Djava.library.path=C:\path\to\opencv\build\java\x64 -jar tangible-programming-1.0-jar-with-dependencies.jar

Oluline on, et käsus oleva java.library.path parameetri väärtuseks antav tee
(path) oleks muudetud vastavalt OpenCV teegi installatsiooni kaustale enda keskkonnas. 
Tee peaks viitama Java liidese jaoks genereeritud .dll teeki sisaldavale kaustale,
mis 64-bitilise operatsioonisüsteemi puhul on vaikimisi opencv\build\java\x64.

### Juhend Linuxi jaoks
Linuxi jaoks ei ole saadaval eelnevalt kompileeritud OpenCV teeki nagu Windowsi jaoks. 
Seetõttu tuleb OpenCV 3.2.0 lähtekoodist ise ehitada. Selle juhendi leiab OpenCV dokumentatsioonist:
http://docs.opencv.org/2.4/doc/tutorials/introduction/linux_install/linux_install.html
Kindlasti tuleks kontrollida enne juhendis toodud make install käsu jooksutamist, 
et Java oleks installitud ning JAVA_HOME keskkonnamuutuja oleks defineeritud
vastavalt Java JDK installatsiooni kaustale. Lisaks on vaja veenduda, et Apache Ant teek oleks installitud. 
Eelnevate puudumisel ei ehitata Java jaoks vajalikku OpenCV liidest.
Pärast edukat make install käsu jooksutamist tekivad kompileerimise väljundkausta lib/ kataloogi vajalikud .so laiendiga teegid. 
Seejärel saab rakendust jooksutada sama käsuga nagu Windowsis. 
Siin tuleks java.library.path parameetri väärtuseks anda eelnevalt tekkinud lib/ kausta tee.

### Rakenduse ehitamine lähtekoodist
Rakenduse saab edukalt lähtekoodist kokku panna ka oma keskkonnas. 
Selleks on tarvis, et oleks Maven 3.x eelnevalt installitud ning PATH keskkonnamuutujas saadaval. 
Projekti kompileerimiseks ning kokkupakkimiseks on eelnevalt tarvis lokaalsesse Maveni repositooriumisse 
installeerida kaks vajalikku sõltuvust, mis on projektiga kaasa pandud. 
Selleks tuleb projekti juurkataloogis jooksutada järgmisi käske:

mvn install:install-file -Dfile=./lib/topcodes.jar -DgroupId=topcodes -DartifactId=topcodes -Dversion=1.0.0 -Dpackaging=jar

mvn install:install-file -Dfile=./lib/opencv-320.jar -DgroupId=org.opencv -DartifactId=opencv -Dversion=3.2.0 -Dpackaging=jar

Pärast nende sõltuvuste installimist saab rakenduse kokku ehitada järgmist käsku
projekti juurkataloogis jooksutades:

mvn clean package

Eelneva tulemusel tekib target/ kausta kaks .jar laiendiga faili, millest tangible-programming-1.0-jar-with-dependencies.jar
sisaldab ka vajalikke TopCode ning OpenCV teekide sõltuvusi.

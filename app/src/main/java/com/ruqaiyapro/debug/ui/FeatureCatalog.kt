package com.ruqaiyapro.debug.ui

/** Honest status of every feature from Boss Rubel's 107 + 5 GOD list. */
data class FeatureItem(val id: String, val name: String, val status: String, val note: String)

object FeatureCatalog {
    // id|name|status(R=real,P=partial,N=not yet)|note
    private val raw = """
G1|GOD #1 Self-Update|P|Prompt > AI code > GitHub commit > Actions build > APK download. Phone-e Gradle chole na.
G2|GOD #2 Brain ON (multi-AI key)|R|Settings > Universal Brain
G3|GOD #3 Auto-Fix Engine|P|Logcat + AI fix suggestion + commit (Update tab)
G4|GOD #4 HOT 12 Play Tuner|P|RAM monitor, 1% saver, largeHeap. Boot/RAM-kill nei.
G5|GOD #5 Live Companion|P|Mic + voice commands + HUD + Chibi
1|Offline Vosk Recognition|P|System speech engine use hoy; Vosk model nei
2|Battery 1% Ultra Saver|R|Screen 1% + RAM poll 60s
3|Screen-off background listening|N|Wakelock service ekhono nei
4|No-Beep trick (900ms mute)|R|6 stream mute/unmute
5|Quick voice triggers|R|Hub mic + chips
6|Live HUD logs|R|Hub
7|Master volume 65%|R|Control
8|Screen brightness 70%|R|Control
9|largeHeap safe mode|R|Manifest
10|Try-catch everywhere|R|
11|RAM check every 15s|R|60s in Ultra Saver
12|WorkManager background|N|Dependency ache, job nei
13|SQLite (Room) khata & logs|R|
14|Runtime permission handler|R|Mic, Camera, Overlay, Storage
15|XOS overlay safe|R|Overlay permission check
16|ML Kit face|P|Face detect (identity match noy)
17|Amake Mone Rakho|P|Owner chobi save
18|Boss vs Unknown detect|N|Face recognition model lage
19|Visual feedback toast|P|Status pill + toast
20|Anti-theft protection|N|
21|Find My Phone (unknown face)|N|
22|CameraX preview|R|Face tab
23|WiFi control|P|Android 10+: panel khole
24|Bluetooth control|P|Settings khole
25|Flashlight + GXC strobe|R|Control
26|Camera switch front/back|R|Face
27|Auto face capture 15s|R|Face thakle
28|Face gallery|R|
29|Owner name in prefs|R|
30|Face confidence 0.7|N|
31|Driving mode hands-free|N|
32|HOT 12 Play hardware tuner|P|Device info + RAM
33|Chibi pinch 40-300dp|R|
34|Chibi drag save x/y|R|
35|Chibi tap bounce + vibrate 50ms|R|
36|App open (camera, WhatsApp, dialer)|P|Voice command
37|Torch on/off command|R|Flash jalo/off
38|What time is it|R|
39|What date is it|R|
40|Roast koro|R|AI ba local
41|Shayari bolo|R|AI ba local
42|Koutuk bolo|R|AI ba local
43|Amar naam ki|R|
44|Phone kor|P|Dialer khole
45|WhatsApp auto ON|N|Accessibility logic nei (stub)
46|WhatsApp loop broadcast|N|Ban-er jhuki, kora hoyni
47|WhatsApp sobaike broadcast bolo|N|
48|Notification listener WhatsApp|N|
49|Auto blur|N|
50|Accessibility auto-type|N|Service stub matro
51|Busy auto-reply lobby|N|
52|Volume master control|R|
53|Brightness control|R|
54|RAM 4+3GB info|R|Hub
55|Code Help & Auto Fix (Logcat)|P|Update tab
56|BadActivity 3 runtime errors|P|Log analyzer generic
57|TERMUX CHIBI ADD|P|Gallery picker recommended
58|Reload Chibi button|R|
59|Reset to default|R|
60|Chibi load priority paths|R|
61|Custom Chibi toast|R|
62|Gallery fallback pic change|R|
63|Gesture: 1 finger drag|R|
64|Gesture: 2 finger pinch|R|
65|Long press 500ms toast|R|
66|Persistence (x,y,size)|R|
67|TTS female pitch 1.15|R|
68|Vibrator 50ms|R|
69|Live transcript (Tumi bolle/Ruqaiya)|R|
70|Vosk small model|N|
71|CameraX 1.3.1|R|
72|ML Kit Face 16.1.5|R|
73|Vosk 0.3.45|N|
74|WorkManager 2.9.0|N|
75|SQLite logs|R|
76|Compose black + green theme|R|
77|Chips (camera/time/torch)|R|
78|Mic 90dp green|R|
79|Transcript field|R|
80|Amake Mone Rakho button|R|
81|Chibi Pic Change + Termux text|R|
82|Brain card + auto fix|R|
83|107 list searchable|R|You are here
84|Battery saver throttle|P|RAM poll slower
85|Screen-off listening|N|
86|Auto start on boot|N|
87|RAM saver >500MB|P|Warning + GC
88|No-crash try-catch|R|
89|HOT 12 Play target|R|
90|4+3GB / 720x1640 tuned|R|
91|minSdk26 target30|R|compile 34
92|largeHeap + legacy storage|R|
93|No FGS microphone|R|
94|Libs (CameraX, MLKit, Room)|P|Vosk/WorkManager unused
95|Proguard keep|R|
96|Accessibility config|R|Config only
97|Settings + 107 search|R|
98|Box blur|N|
99|Dispatchers.IO|R|
100|Toast custom position|P|
101|Wakelock safe release|N|
102|Notification channel|R|
103|START_STICKY foreground|N|
104|900ms delay optimization|R|
105|Google-like HUD|R|
106|Android Studio project ZIP|R|
107|Termux cp chibi|P|Android 11 restriction
""".trimIndent()

    val all: List<FeatureItem> by lazy {
        raw.lines().mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 3) null else FeatureItem(p[0], p[1], p[2], p.getOrElse(3) { "" })
        }
    }
}

package io.tvhome.launcher;

import java.util.Arrays;
import java.util.List;

final class VerseCatalog {
    static final int AYAT = 0;
    static final int HADIS = 1;

    static final class Entry {
        final String id;
        final int type;
        final String arabic;
        final String translation;
        final String source;

        Entry(String id, int type, String arabic, String translation, String source) {
            this.id = id;
            this.type = type;
            this.arabic = arabic;
            this.translation = translation;
            this.source = source;
        }

        String typeName() {
            return type == AYAT ? "Ayat" : "Hadis";
        }
    }

    private static final List<Entry> ENTRIES = Arrays.asList(
            new Entry("ar-rad-28", AYAT,
                    "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                    "Ingatlah, hanya dengan mengingat Allah hati menjadi tenang.",
                    "QS. Ar-Ra'd/13:28"),
            new Entry("al-insyirah-5", AYAT,
                    "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا",
                    "Maka sesungguhnya bersama kesulitan ada kemudahan. Sesungguhnya bersama kesulitan ada kemudahan.",
                    "QS. Al-Insyirah/94:5-6"),
            new Entry("at-talaq-3", AYAT,
                    "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
                    "Dan barangsiapa bertawakal kepada Allah, niscaya Allah akan mencukupkan keperluannya.",
                    "QS. At-Talaq/65:3"),
            new Entry("ali-imran-159", AYAT,
                    "فَإِذَا عَزَمْتَ فَتَوَكَّلْ عَلَى اللَّهِ ۚ إِنَّ اللَّهَ يُحِبُّ الْمُتَوَكِّلِينَ",
                    "Apabila engkau telah membulatkan tekad, bertawakallah kepada Allah. Sesungguhnya Allah mencintai orang-orang yang bertawakal.",
                    "QS. Ali Imran/3:159"),
            new Entry("al-baqarah-152", AYAT,
                    "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
                    "Maka ingatlah kepada-Ku, Aku pun akan ingat kepadamu. Bersyukurlah kepada-Ku dan janganlah kamu ingkar kepada-Ku.",
                    "QS. Al-Baqarah/2:152"),
            new Entry("al-baqarah-286", AYAT,
                    "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
                    "Allah tidak membebani seseorang melainkan sesuai dengan kesanggupannya.",
                    "QS. Al-Baqarah/2:286"),
            new Entry("bukhari-quran", HADIS,
                    "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
                    "Sebaik-baik kalian adalah orang yang mempelajari Al-Qur'an dan mengajarkannya.",
                    "HR. Bukhari"),
            new Entry("tirmidzi-senyum", HADIS,
                    "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ",
                    "Senyummu kepada saudaramu adalah sedekah.",
                    "HR. Tirmidzi"),
            new Entry("muslim-ilmu", HADIS,
                    "مَنْ سَلَكَ طَرِيقًا يَطْلُبُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
                    "Barangsiapa menempuh jalan untuk mencari ilmu, Allah akan memudahkan baginya jalan menuju surga.",
                    "HR. Muslim"),
            new Entry("bukhari-niat", HADIS,
                    "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
                    "Sesungguhnya amal itu tergantung pada niatnya, dan setiap orang akan mendapat sesuai dengan apa yang diniatkannya.",
                    "HR. Bukhari & Muslim"),
            new Entry("bukhari-ikhlas", HADIS,
                    "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
                    "Tidak sempurna iman seseorang di antara kalian hingga ia mencintai saudaranya seperti ia mencintai dirinya sendiri.",
                    "HR. Bukhari & Muslim"),
            new Entry("muslim-kebersihan", HADIS,
                    "الطُّهُورُ شَطْرُ الإِيمَانِ",
                    "Kebersihan adalah sebagian dari iman.",
                    "HR. Muslim"));

    static List<Entry> entries() {
        return ENTRIES;
    }

    static Entry byId(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id.equals(id)) return entry;
        }
        return null;
    }

    private VerseCatalog() {}
}

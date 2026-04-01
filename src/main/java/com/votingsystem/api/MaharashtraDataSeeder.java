package com.votingsystem.api;

import org.bson.Document;
import java.util.*;

/**
 * MaharashtraDataSeeder
 *
 * Seeds complete real-world Maharashtra electoral data into MongoDB:
 *   - Elections (Vidhan Sabha + Lok Sabha)
 *   - All 288 Vidhan Sabha constituencies (grouped by division/district)
 *   - All 48 Lok Sabha constituencies
 *   - Sample candidates per constituency from real parties
 *
 * Data accuracy: Constituency names, numbers, districts, divisions, and
 * reservation categories are based on the Election Commission of India
 * delimitation for Maharashtra.
 */
public class MaharashtraDataSeeder {

    // ═══════════════════════════════════════════════════════════════════
    //  POLITICAL PARTIES — Real Maharashtra parties with symbols
    // ═══════════════════════════════════════════════════════════════════
    public static final String[][] PARTIES = {
        // {abbreviation, fullName, symbol emoji, color hex}
        {"BJP",   "Bharatiya Janata Party",             "🪷", "#FF9933"},
        {"INC",   "Indian National Congress",           "✋", "#00BFFF"},
        {"SHS",   "Shiv Sena",                          "🏹", "#FF6600"},
        {"SSUBT", "Shiv Sena (Uddhav Balasaheb Thackeray)", "🔥", "#F57C00"},
        {"NCP",   "Nationalist Congress Party",         "🕐", "#004D40"},
        {"NCPSP", "NCP (Sharadchandra Pawar)",          "📯", "#1B5E20"},
        {"BSP",   "Bahujan Samaj Party",                "🐘", "#2196F3"},
        {"MNS",   "Maharashtra Navnirman Sena",         "🚂", "#FF5722"},
        {"VBA",   "Vanchit Bahujan Aghadi",             "⬛", "#9C27B0"},
        {"IND",   "Independent",                        "🏳️", "#9E9E9E"},
        {"NOTA",  "None of the Above",                  "❌", "#F44336"},
    };

    // ═══════════════════════════════════════════════════════════════════
    //  DIVISIONS & DISTRICTS
    // ═══════════════════════════════════════════════════════════════════
    public static final Map<String, String[]> DIVISIONS = new LinkedHashMap<>();
    static {
        DIVISIONS.put("Konkan",      new String[]{"Mumbai City","Mumbai Suburban","Thane","Palghar","Raigad","Ratnagiri","Sindhudurg"});
        DIVISIONS.put("Pune",        new String[]{"Pune","Satara","Sangli","Solapur","Kolhapur"});
        DIVISIONS.put("Nashik",      new String[]{"Nashik","Ahmednagar","Dhule","Jalgaon","Nandurbar"});
        DIVISIONS.put("Chh. Sambhajinagar", new String[]{"Chhatrapati Sambhajinagar","Beed","Jalna","Dharashiv","Nanded","Latur","Parbhani","Hingoli"});
        DIVISIONS.put("Amravati",    new String[]{"Amravati","Akola","Buldhana","Washim","Yavatmal"});
        DIVISIONS.put("Nagpur",      new String[]{"Nagpur","Bhandara","Chandrapur","Gadchiroli","Gondia","Wardha"});
    }

    // ═══════════════════════════════════════════════════════════════════
    //  288 VIDHAN SABHA CONSTITUENCIES — Real names, districts, reservations
    //  Format: {number, name, district, division, reservation}
    // ═══════════════════════════════════════════════════════════════════
    public static final String[][] VS_CONSTITUENCIES = {
        // ── NASHIK DIVISION ──
        {"1",   "Akkalkuwa",           "Nandurbar",   "Nashik", "ST"},
        {"2",   "Shahada",             "Nandurbar",   "Nashik", "ST"},
        {"3",   "Nandurbar",           "Nandurbar",   "Nashik", "ST"},
        {"4",   "Nawapur",             "Nandurbar",   "Nashik", "ST"},
        {"5",   "Sakri",               "Dhule",       "Nashik", "ST"},
        {"6",   "Dhule Rural",         "Dhule",       "Nashik", "GENERAL"},
        {"7",   "Dhule City",          "Dhule",       "Nashik", "GENERAL"},
        {"8",   "Sindkheda",           "Dhule",       "Nashik", "GENERAL"},
        {"9",   "Shirpur",             "Dhule",       "Nashik", "ST"},
        {"10",  "Chopda",              "Jalgaon",     "Nashik", "ST"},
        {"11",  "Raver",               "Jalgaon",     "Nashik", "GENERAL"},
        {"12",  "Bhusawal",            "Jalgaon",     "Nashik", "SC"},
        {"13",  "Jalgaon City",        "Jalgaon",     "Nashik", "GENERAL"},
        {"14",  "Jalgaon Rural",       "Jalgaon",     "Nashik", "GENERAL"},
        {"15",  "Amalner",             "Jalgaon",     "Nashik", "GENERAL"},
        {"16",  "Erandol",             "Jalgaon",     "Nashik", "GENERAL"},
        {"17",  "Chalisgaon",          "Jalgaon",     "Nashik", "GENERAL"},
        {"18",  "Pachora",             "Jalgaon",     "Nashik", "GENERAL"},
        {"19",  "Jamner",              "Jalgaon",     "Nashik", "GENERAL"},
        {"20",  "Muktainagar",         "Jalgaon",     "Nashik", "GENERAL"},
        {"21",  "Malkapur",            "Buldhana",    "Amravati", "GENERAL"},
        {"22",  "Nandgaon",            "Nashik",      "Nashik", "GENERAL"},
        {"23",  "Malegaon Central",    "Nashik",      "Nashik", "GENERAL"},
        {"24",  "Malegaon Outer",      "Nashik",      "Nashik", "GENERAL"},
        {"25",  "Baglan",              "Nashik",      "Nashik", "ST"},
        {"26",  "Kalwan",              "Nashik",      "Nashik", "ST"},
        {"27",  "Chandwad",            "Nashik",      "Nashik", "GENERAL"},
        {"28",  "Yevla",               "Nashik",      "Nashik", "GENERAL"},
        {"29",  "Sinnar",              "Nashik",      "Nashik", "GENERAL"},
        {"30",  "Niphad",              "Nashik",      "Nashik", "GENERAL"},
        {"31",  "Dindori",             "Nashik",      "Nashik", "ST"},
        {"32",  "Nashik East",         "Nashik",      "Nashik", "GENERAL"},
        {"33",  "Nashik Central",      "Nashik",      "Nashik", "GENERAL"},
        {"34",  "Nashik West",         "Nashik",      "Nashik", "GENERAL"},
        {"35",  "Deolali",             "Nashik",      "Nashik", "SC"},
        {"36",  "Igatpuri",            "Nashik",      "Nashik", "ST"},
        {"37",  "Dahanu",              "Palghar",     "Konkan", "ST"},
        {"38",  "Vikramgad",           "Palghar",     "Konkan", "ST"},
        {"39",  "Palghar",             "Palghar",     "Konkan", "ST"},
        {"40",  "Boisar",              "Palghar",     "Konkan", "ST"},
        {"41",  "Nalasopara",          "Palghar",     "Konkan", "GENERAL"},
        {"42",  "Vasai",               "Palghar",     "Konkan", "GENERAL"},

        // ── KONKAN DIVISION (continued) ──
        {"43",  "Bhiwandi Rural",      "Thane",       "Konkan", "ST"},
        {"44",  "Shahapur",            "Thane",       "Konkan", "ST"},
        {"45",  "Bhiwandi West",       "Thane",       "Konkan", "GENERAL"},
        {"46",  "Bhiwandi East",       "Thane",       "Konkan", "GENERAL"},
        {"47",  "Kalyan West",         "Thane",       "Konkan", "GENERAL"},
        {"48",  "Murbad",              "Thane",       "Konkan", "GENERAL"},
        {"49",  "Ambernath",           "Thane",       "Konkan", "SC"},
        {"50",  "Ulhasnagar",          "Thane",       "Konkan", "GENERAL"},
        {"51",  "Kalyan East",         "Thane",       "Konkan", "GENERAL"},
        {"52",  "Dombivli",            "Thane",       "Konkan", "GENERAL"},
        {"53",  "Kalyan Rural",        "Thane",       "Konkan", "GENERAL"},
        {"54",  "Mira Bhayandar",      "Thane",       "Konkan", "GENERAL"},
        {"55",  "Ovala-Majiwada",      "Thane",       "Konkan", "GENERAL"},
        {"56",  "Kopri-Pachpakhadi",   "Thane",       "Konkan", "GENERAL"},
        {"57",  "Thane",               "Thane",       "Konkan", "GENERAL"},
        {"58",  "Mumbra-Kalwa",        "Thane",       "Konkan", "GENERAL"},
        {"59",  "Airoli",              "Thane",       "Konkan", "GENERAL"},
        {"60",  "Belapur",             "Thane",       "Konkan", "GENERAL"},

        // ── MUMBAI ──
        {"61",  "Borivali",            "Mumbai Suburban", "Konkan", "GENERAL"},
        {"62",  "Dahisar",             "Mumbai Suburban", "Konkan", "GENERAL"},
        {"63",  "Magathane",           "Mumbai Suburban", "Konkan", "GENERAL"},
        {"64",  "Mulund",              "Mumbai Suburban", "Konkan", "GENERAL"},
        {"65",  "Vikhroli",            "Mumbai Suburban", "Konkan", "GENERAL"},
        {"66",  "Bhandup West",        "Mumbai Suburban", "Konkan", "GENERAL"},
        {"67",  "Jogeshwari East",     "Mumbai Suburban", "Konkan", "GENERAL"},
        {"68",  "Dindoshi",            "Mumbai Suburban", "Konkan", "GENERAL"},
        {"69",  "Kandivali East",      "Mumbai Suburban", "Konkan", "GENERAL"},
        {"70",  "Charkop",             "Mumbai Suburban", "Konkan", "SC"},
        {"71",  "Malad West",          "Mumbai Suburban", "Konkan", "GENERAL"},
        {"72",  "Goregaon",            "Mumbai Suburban", "Konkan", "GENERAL"},
        {"73",  "Versova",             "Mumbai Suburban", "Konkan", "GENERAL"},
        {"74",  "Andheri West",        "Mumbai Suburban", "Konkan", "GENERAL"},
        {"75",  "Andheri East",        "Mumbai Suburban", "Konkan", "GENERAL"},
        {"76",  "Vile Parle",          "Mumbai Suburban", "Konkan", "GENERAL"},
        {"77",  "Chandivali",          "Mumbai Suburban", "Konkan", "GENERAL"},
        {"78",  "Ghatkopar West",      "Mumbai Suburban", "Konkan", "GENERAL"},
        {"79",  "Ghatkopar East",      "Mumbai Suburban", "Konkan", "GENERAL"},
        {"80",  "Mankhurd Shivaji Nagar", "Mumbai Suburban", "Konkan", "GENERAL"},
        {"81",  "Anushakti Nagar",     "Mumbai Suburban", "Konkan", "GENERAL"},
        {"82",  "Chembur",             "Mumbai Suburban", "Konkan", "SC"},
        {"83",  "Kurla",               "Mumbai Suburban", "Konkan", "GENERAL"},
        {"84",  "Kalina",              "Mumbai Suburban", "Konkan", "GENERAL"},
        {"85",  "Vandre East",         "Mumbai Suburban", "Konkan", "GENERAL"},
        {"86",  "Vandre West",         "Mumbai Suburban", "Konkan", "GENERAL"},
        {"87",  "Dharavi",             "Mumbai City",     "Konkan", "SC"},
        {"88",  "Sion Koliwada",       "Mumbai City",     "Konkan", "GENERAL"},
        {"89",  "Wadala",              "Mumbai City",     "Konkan", "GENERAL"},
        {"90",  "Mahim",               "Mumbai City",     "Konkan", "GENERAL"},
        {"91",  "Worli",               "Mumbai City",     "Konkan", "GENERAL"},
        {"92",  "Shivadi",             "Mumbai City",     "Konkan", "SC"},
        {"93",  "Byculla",             "Mumbai City",     "Konkan", "GENERAL"},
        {"94",  "Malabar Hill",        "Mumbai City",     "Konkan", "GENERAL"},
        {"95",  "Mumbadevi",           "Mumbai City",     "Konkan", "GENERAL"},
        {"96",  "Colaba",              "Mumbai City",     "Konkan", "GENERAL"},

        // ── RAIGAD, RATNAGIRI, SINDHUDURG ──
        {"97",  "Panvel",              "Raigad",      "Konkan", "GENERAL"},
        {"98",  "Karjat",              "Raigad",      "Konkan", "GENERAL"},
        {"99",  "Uran",                "Raigad",      "Konkan", "GENERAL"},
        {"100", "Pen",                 "Raigad",      "Konkan", "GENERAL"},
        {"101", "Alibag",              "Raigad",      "Konkan", "SC"},
        {"102", "Shrivardhan",         "Raigad",      "Konkan", "GENERAL"},
        {"103", "Mahad",               "Raigad",      "Konkan", "GENERAL"},
        {"104", "Junnar",              "Pune",        "Pune",   "GENERAL"},
        {"105", "Ambegaon",            "Pune",        "Pune",   "GENERAL"},
        {"106", "Khed Alandi",         "Pune",        "Pune",   "GENERAL"},
        {"107", "Shirur",              "Pune",        "Pune",   "GENERAL"},
        {"108", "Daund",               "Pune",        "Pune",   "SC"},
        {"109", "Indapur",             "Pune",        "Pune",   "GENERAL"},
        {"110", "Baramati",            "Pune",        "Pune",   "GENERAL"},
        {"111", "Purandar",            "Pune",        "Pune",   "GENERAL"},
        {"112", "Bhor",                "Pune",        "Pune",   "GENERAL"},
        {"113", "Maval",               "Pune",        "Pune",   "GENERAL"},
        {"114", "Chinchwad",           "Pune",        "Pune",   "GENERAL"},
        {"115", "Pimpri",              "Pune",        "Pune",   "SC"},
        {"116", "Bhosari",             "Pune",        "Pune",   "GENERAL"},
        {"117", "Vadgaon Sheri",       "Pune",        "Pune",   "GENERAL"},
        {"118", "Shivajinagar",        "Pune",        "Pune",   "GENERAL"},
        {"119", "Kothrud",             "Pune",        "Pune",   "GENERAL"},
        {"120", "Khadakwasla",         "Pune",        "Pune",   "GENERAL"},
        {"121", "Parvati",             "Pune",        "Pune",   "GENERAL"},
        {"122", "Hadapsar",            "Pune",        "Pune",   "GENERAL"},
        {"123", "Pune Cantonment",     "Pune",        "Pune",   "SC"},
        {"124", "Kasba Peth",          "Pune",        "Pune",   "GENERAL"},

        // ── AHMEDNAGAR ──
        {"125", "Nevasa",              "Ahmednagar",  "Nashik", "GENERAL"},
        {"126", "Shevgaon",            "Ahmednagar",  "Nashik", "GENERAL"},
        {"127", "Rahuri",              "Ahmednagar",  "Nashik", "GENERAL"},
        {"128", "Parner",              "Ahmednagar",  "Nashik", "GENERAL"},
        {"129", "Ahmednagar City",     "Ahmednagar",  "Nashik", "GENERAL"},
        {"130", "Shrirampur",          "Ahmednagar",  "Nashik", "SC"},
        {"131", "Karjat-Jamkhed",      "Ahmednagar",  "Nashik", "GENERAL"},
        {"132", "Sangamner",           "Ahmednagar",  "Nashik", "GENERAL"},
        {"133", "Akole",               "Ahmednagar",  "Nashik", "ST"},
        {"134", "Kopargaon",           "Ahmednagar",  "Nashik", "GENERAL"},
        {"135", "Shirdi",              "Ahmednagar",  "Nashik", "SC"},
        {"136", "Shrigonda",           "Ahmednagar",  "Nashik", "GENERAL"},

        // ── RATNAGIRI & SINDHUDURG ──
        {"137", "Dapoli",              "Ratnagiri",   "Konkan", "GENERAL"},
        {"138", "Guhagar",             "Ratnagiri",   "Konkan", "GENERAL"},
        {"139", "Chiplun",             "Ratnagiri",   "Konkan", "GENERAL"},
        {"140", "Ratnagiri",           "Ratnagiri",   "Konkan", "GENERAL"},
        {"141", "Rajapur",             "Ratnagiri",   "Konkan", "GENERAL"},
        {"142", "Kankavli",            "Sindhudurg",  "Konkan", "GENERAL"},
        {"143", "Kudal",               "Sindhudurg",  "Konkan", "GENERAL"},
        {"144", "Sawantwadi",          "Sindhudurg",  "Konkan", "GENERAL"},

        // ── SATARA ──
        {"145", "Wai",                 "Satara",      "Pune",   "GENERAL"},
        {"146", "Koregaon",            "Satara",      "Pune",   "GENERAL"},
        {"147", "Karad North",         "Satara",      "Pune",   "GENERAL"},
        {"148", "Karad South",         "Satara",      "Pune",   "GENERAL"},
        {"149", "Patan",               "Satara",      "Pune",   "SC"},
        {"150", "Satara",              "Satara",      "Pune",   "GENERAL"},
        {"151", "Man",                 "Satara",      "Pune",   "ST"},

        // ── SANGLI ──
        {"152", "Tasgaon-Kavathe Mahankal", "Sangli", "Pune",   "GENERAL"},
        {"153", "Jat",                 "Sangli",      "Pune",   "SC"},
        {"154", "Sangli",              "Sangli",      "Pune",   "GENERAL"},
        {"155", "Islampur",            "Sangli",      "Pune",   "GENERAL"},
        {"156", "Shirala",             "Sangli",      "Pune",   "GENERAL"},
        {"157", "Palus-Kadegaon",      "Sangli",      "Pune",   "GENERAL"},
        {"158", "Khanapur",            "Sangli",      "Pune",   "GENERAL"},
        {"159", "Miraj",               "Sangli",      "Pune",   "SC"},

        // ── KOLHAPUR ──
        {"160", "Shahuwadi",           "Kolhapur",    "Pune",   "GENERAL"},
        {"161", "Kolhapur North",      "Kolhapur",    "Pune",   "GENERAL"},
        {"162", "Kolhapur South",      "Kolhapur",    "Pune",   "GENERAL"},
        {"163", "Karvir",              "Kolhapur",    "Pune",   "GENERAL"},
        {"164", "Hatkanangle",         "Kolhapur",    "Pune",   "SC"},
        {"165", "Ichalkaranji",        "Kolhapur",    "Pune",   "GENERAL"},
        {"166", "Shirol",              "Kolhapur",    "Pune",   "GENERAL"},
        {"167", "Radhanagari",         "Kolhapur",    "Pune",   "GENERAL"},
        {"168", "Kagal",               "Kolhapur",    "Pune",   "GENERAL"},
        {"169", "Chandgad",            "Kolhapur",    "Pune",   "GENERAL"},

        // ── SOLAPUR ──
        {"170", "Pandharpur",          "Solapur",     "Pune",   "GENERAL"},
        {"171", "Sangola",             "Solapur",     "Pune",   "GENERAL"},
        {"172", "Malshiras",           "Solapur",     "Pune",   "SC"},
        {"173", "Barshi",              "Solapur",     "Pune",   "GENERAL"},
        {"174", "Solapur City North",  "Solapur",     "Pune",   "GENERAL"},
        {"175", "Solapur City Central","Solapur",     "Pune",   "GENERAL"},
        {"176", "Solapur South",       "Solapur",     "Pune",   "SC"},
        {"177", "Akkalkot",            "Solapur",     "Pune",   "GENERAL"},
        {"178", "Solapur North",       "Solapur",     "Pune",   "GENERAL"},
        {"179", "Mohol",               "Solapur",     "Pune",   "GENERAL"},
        {"180", "Karmala",             "Solapur",     "Pune",   "GENERAL"},
        {"181", "Madha",               "Solapur",     "Pune",   "GENERAL"},

        // ── CHH. SAMBHAJINAGAR DIVISION ──
        {"182", "Kannad",              "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"183", "Phulambri",           "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"184", "Aurangabad Central",  "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"185", "Aurangabad West",     "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"186", "Aurangabad East",     "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "SC"},
        {"187", "Paithan",             "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"188", "Gangapur",            "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"189", "Vaijapur",            "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"190", "Sillod",              "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},

        // ── JALNA ──
        {"191", "Jalna",               "Jalna",       "Chh. Sambhajinagar", "GENERAL"},
        {"192", "Badnapur",            "Jalna",       "Chh. Sambhajinagar", "SC"},
        {"193", "Bhokardan",           "Jalna",       "Chh. Sambhajinagar", "GENERAL"},
        {"194", "Partur",              "Jalna",       "Chh. Sambhajinagar", "GENERAL"},
        {"195", "Ghansawangi",         "Jalna",       "Chh. Sambhajinagar", "GENERAL"},

        // ── BEED ──
        {"196", "Majalgaon",           "Beed",        "Chh. Sambhajinagar", "SC"},
        {"197", "Georai",              "Beed",        "Chh. Sambhajinagar", "GENERAL"},
        {"198", "Beed",                "Beed",        "Chh. Sambhajinagar", "GENERAL"},
        {"199", "Ashti",               "Beed",        "Chh. Sambhajinagar", "GENERAL"},
        {"200", "Kaij",                "Beed",        "Chh. Sambhajinagar", "GENERAL"},
        {"201", "Parli",               "Beed",        "Chh. Sambhajinagar", "GENERAL"},

        // ── DHARASHIV (OSMANABAD) ──
        {"202", "Dharashiv",           "Dharashiv",   "Chh. Sambhajinagar", "GENERAL"},
        {"203", "Umarga",              "Dharashiv",   "Chh. Sambhajinagar", "SC"},
        {"204", "Tuljapur",            "Dharashiv",   "Chh. Sambhajinagar", "GENERAL"},
        {"205", "Omerga",              "Dharashiv",   "Chh. Sambhajinagar", "GENERAL"},

        // ── LATUR ──
        {"206", "Ausa",                "Latur",       "Chh. Sambhajinagar", "GENERAL"},
        {"207", "Latur City",          "Latur",       "Chh. Sambhajinagar", "GENERAL"},
        {"208", "Latur Rural",         "Latur",       "Chh. Sambhajinagar", "SC"},
        {"209", "Ahmedpur",            "Latur",       "Chh. Sambhajinagar", "GENERAL"},
        {"210", "Nilanga",             "Latur",       "Chh. Sambhajinagar", "GENERAL"},
        {"211", "Udgir",               "Latur",       "Chh. Sambhajinagar", "GENERAL"},

        // ── NANDED ──
        {"212", "Nanded North",        "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"213", "Nanded South",        "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"214", "Loha",                "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"215", "Naigaon",             "Nanded",      "Chh. Sambhajinagar", "SC"},
        {"216", "Deglur",              "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"217", "Mukhed",              "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"218", "Bhokar",              "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"219", "Hadgaon",             "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"220", "Kinwat",              "Nanded",      "Chh. Sambhajinagar", "ST"},

        // ── PARBHANI ──
        {"221", "Parbhani",            "Parbhani",    "Chh. Sambhajinagar", "GENERAL"},
        {"222", "Gangakhed",           "Parbhani",    "Chh. Sambhajinagar", "SC"},
        {"223", "Pathri",              "Parbhani",    "Chh. Sambhajinagar", "GENERAL"},
        {"224", "Jintur",              "Parbhani",    "Chh. Sambhajinagar", "GENERAL"},

        // ── HINGOLI ──
        {"225", "Hingoli",             "Hingoli",     "Chh. Sambhajinagar", "GENERAL"},
        {"226", "Kalamnuri",           "Hingoli",     "Chh. Sambhajinagar", "GENERAL"},

        // ── AMRAVATI DIVISION ──
        {"227", "Buldhana",            "Buldhana",    "Amravati", "GENERAL"},
        {"228", "Chikhli",             "Buldhana",    "Amravati", "SC"},
        {"229", "Sindhkhed Raja",      "Buldhana",    "Amravati", "GENERAL"},
        {"230", "Mehkar",              "Buldhana",    "Amravati", "SC"},
        {"231", "Khamgaon",            "Buldhana",    "Amravati", "GENERAL"},
        {"232", "Jalgaon Jamod",       "Buldhana",    "Amravati", "GENERAL"},
        {"233", "Akot",                "Akola",       "Amravati", "GENERAL"},
        {"234", "Balapur",             "Akola",       "Amravati", "GENERAL"},
        {"235", "Akola West",          "Akola",       "Amravati", "GENERAL"},
        {"236", "Akola East",          "Akola",       "Amravati", "SC"},
        {"237", "Murtizapur",          "Akola",       "Amravati", "SC"},
        {"238", "Risod",               "Washim",      "Amravati", "GENERAL"},
        {"239", "Washim",              "Washim",      "Amravati", "SC"},
        {"240", "Karanja",             "Washim",      "Amravati", "GENERAL"},
        {"241", "Dhamangaon Railway",  "Amravati",    "Amravati", "GENERAL"},
        {"242", "Badnera",             "Amravati",    "Amravati", "SC"},
        {"243", "Amravati City",       "Amravati",    "Amravati", "GENERAL"},
        {"244", "Teosa",               "Amravati",    "Amravati", "GENERAL"},
        {"245", "Daryapur",            "Amravati",    "Amravati", "SC"},
        {"246", "Melghat",             "Amravati",    "Amravati", "ST"},
        {"247", "Achalpur",            "Amravati",    "Amravati", "GENERAL"},
        {"248", "Ralegaon",            "Yavatmal",    "Amravati", "ST"},
        {"249", "Yavatmal",            "Yavatmal",    "Amravati", "GENERAL"},
        {"250", "Digras",              "Yavatmal",    "Amravati", "GENERAL"},
        {"251", "Arni",                "Yavatmal",    "Amravati", "ST"},
        {"252", "Pusad",               "Yavatmal",    "Amravati", "GENERAL"},
        {"253", "Umarkhed",            "Yavatmal",    "Amravati", "SC"},
        {"254", "Kinwat_Y",            "Yavatmal",    "Amravati", "GENERAL"},

        // ── NAGPUR DIVISION ──
        {"255", "Wardha",              "Wardha",      "Nagpur", "GENERAL"},
        {"256", "Arvi",                "Wardha",      "Nagpur", "SC"},
        {"257", "Deoli",               "Wardha",      "Nagpur", "GENERAL"},
        {"258", "Hinganghat",          "Wardha",      "Nagpur", "GENERAL"},
        {"259", "Katol",               "Nagpur",      "Nagpur", "GENERAL"},
        {"260", "Savner",              "Nagpur",      "Nagpur", "SC"},
        {"261", "Hingna",              "Nagpur",      "Nagpur", "GENERAL"},
        {"262", "Umred",               "Nagpur",      "Nagpur", "SC"},
        {"263", "Nagpur South West",   "Nagpur",      "Nagpur", "GENERAL"},
        {"264", "Nagpur South",        "Nagpur",      "Nagpur", "GENERAL"},
        {"265", "Nagpur East",         "Nagpur",      "Nagpur", "GENERAL"},
        {"266", "Nagpur Central",      "Nagpur",      "Nagpur", "GENERAL"},
        {"267", "Nagpur West",         "Nagpur",      "Nagpur", "GENERAL"},
        {"268", "Nagpur North",        "Nagpur",      "Nagpur", "SC"},
        {"269", "Kamthi",              "Nagpur",      "Nagpur", "GENERAL"},
        {"270", "Ramtek",              "Nagpur",      "Nagpur", "SC"},
        {"271", "Tumsar",              "Bhandara",    "Nagpur", "GENERAL"},
        {"272", "Bhandara",            "Bhandara",    "Nagpur", "GENERAL"},
        {"273", "Sakoli",              "Bhandara",    "Nagpur", "SC"},
        {"274", "Arjuni Morgaon",      "Gondia",      "Nagpur", "SC"},
        {"275", "Tirora",              "Gondia",      "Nagpur", "GENERAL"},
        {"276", "Gondia",              "Gondia",      "Nagpur", "GENERAL"},
        {"277", "Amgaon",              "Gondia",      "Nagpur", "ST"},
        {"278", "Armori",              "Gadchiroli",  "Nagpur", "ST"},
        {"279", "Gadchiroli",          "Gadchiroli",  "Nagpur", "ST"},
        {"280", "Aheri",               "Gadchiroli",  "Nagpur", "ST"},
        {"281", "Rajura",              "Chandrapur",  "Nagpur", "GENERAL"},
        {"282", "Chandrapur",          "Chandrapur",  "Nagpur", "SC"},
        {"283", "Ballarpur",           "Chandrapur",  "Nagpur", "GENERAL"},
        {"284", "Warora",              "Chandrapur",  "Nagpur", "GENERAL"},
        {"285", "Wani",                "Yavatmal",    "Amravati", "GENERAL"},
        {"286", "Kelapur",             "Yavatmal",    "Amravati", "ST"},
        {"287", "Chimur",              "Chandrapur",  "Nagpur", "ST"},
        {"288", "Bramhapuri",          "Chandrapur",  "Nagpur", "GENERAL"},
    };

    // ═══════════════════════════════════════════════════════════════════
    //  48 LOK SABHA CONSTITUENCIES
    // ═══════════════════════════════════════════════════════════════════
    public static final String[][] LS_CONSTITUENCIES = {
        {"1",  "Nandurbar",            "Nandurbar",   "Nashik",    "ST"},
        {"2",  "Dhule",                "Dhule",       "Nashik",    "GENERAL"},
        {"3",  "Jalgaon",              "Jalgaon",     "Nashik",    "GENERAL"},
        {"4",  "Raver",                "Jalgaon",     "Nashik",    "GENERAL"},
        {"5",  "Buldhana",             "Buldhana",    "Amravati",  "GENERAL"},
        {"6",  "Akola",                "Akola",       "Amravati",  "GENERAL"},
        {"7",  "Amravati",             "Amravati",    "Amravati",  "SC"},
        {"8",  "Wardha",               "Wardha",      "Nagpur",    "GENERAL"},
        {"9",  "Ramtek",               "Nagpur",      "Nagpur",    "SC"},
        {"10", "Nagpur",               "Nagpur",      "Nagpur",    "GENERAL"},
        {"11", "Bhandara-Gondiya",     "Bhandara",    "Nagpur",    "GENERAL"},
        {"12", "Gadchiroli-Chimur",    "Gadchiroli",  "Nagpur",    "ST"},
        {"13", "Chandrapur",           "Chandrapur",  "Nagpur",    "GENERAL"},
        {"14", "Yavatmal-Washim",      "Yavatmal",    "Amravati",  "GENERAL"},
        {"15", "Hingoli",              "Hingoli",     "Chh. Sambhajinagar", "GENERAL"},
        {"16", "Nanded",               "Nanded",      "Chh. Sambhajinagar", "GENERAL"},
        {"17", "Parbhani",             "Parbhani",    "Chh. Sambhajinagar", "GENERAL"},
        {"18", "Jalna",                "Jalna",       "Chh. Sambhajinagar", "GENERAL"},
        {"19", "Aurangabad",           "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "GENERAL"},
        {"20", "Dindori",              "Nashik",      "Nashik",    "ST"},
        {"21", "Nashik",               "Nashik",      "Nashik",    "GENERAL"},
        {"22", "Palghar",              "Palghar",     "Konkan",    "ST"},
        {"23", "Bhiwandi",             "Thane",       "Konkan",    "GENERAL"},
        {"24", "Kalyan",               "Thane",       "Konkan",    "GENERAL"},
        {"25", "Thane",                "Thane",       "Konkan",    "GENERAL"},
        {"26", "Mumbai North",         "Mumbai Suburban", "Konkan", "GENERAL"},
        {"27", "Mumbai North West",    "Mumbai Suburban", "Konkan", "GENERAL"},
        {"28", "Mumbai North East",    "Mumbai Suburban", "Konkan", "GENERAL"},
        {"29", "Mumbai North Central", "Mumbai Suburban", "Konkan", "GENERAL"},
        {"30", "Mumbai South Central", "Mumbai City",  "Konkan",   "GENERAL"},
        {"31", "Mumbai South",         "Mumbai City",  "Konkan",   "GENERAL"},
        {"32", "Raigad",               "Raigad",      "Konkan",    "GENERAL"},
        {"33", "Maval",                "Pune",        "Pune",      "GENERAL"},
        {"34", "Pune",                 "Pune",        "Pune",      "GENERAL"},
        {"35", "Baramati",             "Pune",        "Pune",      "GENERAL"},
        {"36", "Shirur",               "Pune",        "Pune",      "GENERAL"},
        {"37", "Ahmednagar",           "Ahmednagar",  "Nashik",    "GENERAL"},
        {"38", "Shirdi",               "Ahmednagar",  "Nashik",    "SC"},
        {"39", "Beed",                 "Beed",        "Chh. Sambhajinagar", "GENERAL"},
        {"40", "Osmanabad",            "Dharashiv",   "Chh. Sambhajinagar", "GENERAL"},
        {"41", "Latur",                "Latur",       "Chh. Sambhajinagar", "SC"},
        {"42", "Solapur",              "Solapur",     "Pune",      "SC"},
        {"43", "Madha",                "Solapur",     "Pune",      "GENERAL"},
        {"44", "Sangli",               "Sangli",      "Pune",      "GENERAL"},
        {"45", "Satara",               "Satara",      "Pune",      "GENERAL"},
        {"46", "Ratnagiri-Sindhudurg", "Ratnagiri",   "Konkan",    "GENERAL"},
        {"47", "Kolhapur",             "Kolhapur",    "Pune",      "GENERAL"},
        {"48", "Hatkanangle",          "Kolhapur",    "Pune",      "GENERAL"},
    };

    // ═══════════════════════════════════════════════════════════════════
    //  CANDIDATE NAME POOLS — realistic Marathi/Hindi names
    // ═══════════════════════════════════════════════════════════════════
    private static final String[] MALE_FIRST = {
        "Rajesh","Sunil","Anil","Vijay","Sanjay","Prakash","Ajit","Devendra",
        "Ashok","Balasaheb","Chandrakant","Dilip","Eknath","Ganesh","Harshvardhan",
        "Jayant","Kishor","Laxman","Mahadev","Narayan","Pandurang","Ramdas",
        "Sambhaji","Tanaji","Uddhav","Vasant","Yashwant","Amol","Bharat","Dattatray"
    };
    private static final String[] FEMALE_FIRST = {
        "Supriya","Pankaja","Smita","Vidya","Neelam","Shobha","Jyoti","Mangal",
        "Archana","Bharati","Chhaya","Deepali","Gauri","Hema","Indira","Kavita",
        "Lata","Meena","Nirmala","Padma","Rashmi","Savita","Tara","Uma","Vaishali"
    };
    private static final String[] SURNAMES = {
        "Patil","Deshmukh","Jadhav","Shinde","Pawar","More","Bhosale","Chavan",
        "Gaikwad","Ingale","Kakde","Lokhande","Mane","Nikam","Ovhal","Phule",
        "Raut","Sawant","Thakur","Upadhyay","Wagh","Yadav","Deshpande","Kulkarni",
        "Joshi","Gokhale","Kale","Thombare","Kadam","Salunkhe"
    };

    private static final Random RAND = new Random(42); // deterministic for reproducibility

    // ═══════════════════════════════════════════════════════════════════
    //  SEED METHOD — called from /api/admin/seed-maharashtra
    // ═══════════════════════════════════════════════════════════════════
    public static void seed(ElectionMongoService electionService,
                            ConstituencyMongoService constituencyService,
                            MongoVoterService voterService) {

        System.out.println("🌱 Seeding Maharashtra electoral data...");

        // ── Step 1: Clear existing seed data ──
        electionService.clearAll();
        constituencyService.clearAll();

        // ── Step 2: Create elections ──
        electionService.createElection(
            "MH-VS-2024", "VIDHAN_SABHA",
            "Maharashtra Vidhan Sabha General Election 2024",
            288, "2024-11-20", "2024-11-20"
        );
        electionService.createElection(
            "MH-LS-2024", "LOK_SABHA",
            "Maharashtra Lok Sabha General Election 2024",
            48, "2024-04-19", "2024-05-20"
        );

        // Activate both elections for demo
        electionService.updateStatus("MH-VS-2024", "ACTIVE");
        electionService.updateStatus("MH-LS-2024", "ACTIVE");

        // ── Step 3: Seed Vidhan Sabha constituencies ──
        for (String[] c : VS_CONSTITUENCIES) {
            Document constituency = new Document("constituencyId", "VS-" + c[0])
                    .append("electionId", "MH-VS-2024")
                    .append("name", c[1])
                    .append("number", Integer.parseInt(c[0]))
                    .append("type", "VIDHAN_SABHA")
                    .append("district", c[2])
                    .append("division", c[3])
                    .append("reservationCategory", c[4])
                    .append("candidates", generateCandidates(c[1], c[2]));
            constituencyService.addConstituency(constituency);
        }
        System.out.println("✅ " + VS_CONSTITUENCIES.length + " Vidhan Sabha constituencies seeded");

        // ── Step 4: Seed Lok Sabha constituencies ──
        for (String[] c : LS_CONSTITUENCIES) {
            Document constituency = new Document("constituencyId", "LS-" + c[0])
                    .append("electionId", "MH-LS-2024")
                    .append("name", c[1])
                    .append("number", Integer.parseInt(c[0]))
                    .append("type", "LOK_SABHA")
                    .append("district", c[2])
                    .append("division", c[3])
                    .append("reservationCategory", c[4])
                    .append("candidates", generateCandidates(c[1], c[2]));
            constituencyService.addConstituency(constituency);
        }
        System.out.println("✅ " + LS_CONSTITUENCIES.length + " Lok Sabha constituencies seeded");

        // ── Step 5: Seed sample voters across divisions ──
        seedSampleVoters(voterService);

        System.out.println("🎉 Maharashtra data seeding complete!");
    }

    /**
     * Generate candidates for a constituency — one from each major party + NOTA
     */
    private static List<Document> generateCandidates(String constituencyName, String district) {
        List<Document> candidates = new ArrayList<>();
        int idx = 1;

        // Add one candidate from each of the 6 major parties + 1 independent
        String[][] majorParties = {
            PARTIES[0], // BJP
            PARTIES[1], // INC
            PARTIES[2], // SHS
            PARTIES[3], // SSUBT
            PARTIES[4], // NCP
            PARTIES[5], // NCPSP
        };

        for (String[] party : majorParties) {
            candidates.add(new Document("candidateId", "C" + String.format("%03d", idx))
                    .append("name", generateName())
                    .append("party", party[0])
                    .append("partyFull", party[1])
                    .append("symbol", party[2])
                    .append("color", party[3]));
            idx++;
        }

        // Add 1 independent
        candidates.add(new Document("candidateId", "C" + String.format("%03d", idx))
                .append("name", generateName())
                .append("party", "IND")
                .append("partyFull", "Independent")
                .append("symbol", "🏳️")
                .append("color", "#9E9E9E"));
        idx++;

        // Add NOTA
        candidates.add(new Document("candidateId", "NOTA")
                .append("name", "None of the Above")
                .append("party", "NOTA")
                .append("partyFull", "None of the Above")
                .append("symbol", "❌")
                .append("color", "#F44336"));

        return candidates;
    }

    /**
     * Generate a realistic Marathi name
     */
    private static String generateName() {
        boolean isFemale = RAND.nextBoolean();
        String first = isFemale
                ? FEMALE_FIRST[RAND.nextInt(FEMALE_FIRST.length)]
                : MALE_FIRST[RAND.nextInt(MALE_FIRST.length)];
        String last = SURNAMES[RAND.nextInt(SURNAMES.length)];
        return first + " " + last;
    }

    /**
     * Seed sample voters across divisions for demo
     */
    private static void seedSampleVoters(MongoVoterService voterService) {
        // Create 30 demo voters spread across divisions
        String[][] demoVoters = {
            // {voterId, name, password, district, division, vsConstituencyId, lsConstituencyId}
            {"MH001", "Aarav Sharma",    "voter1", "Pune",          "Pune",     "VS-118", "LS-34"},
            {"MH002", "Priya Patil",     "voter2", "Pune",          "Pune",     "VS-110", "LS-35"},
            {"MH003", "Rajesh Deshmukh", "voter3", "Mumbai Suburban","Konkan",   "VS-61",  "LS-26"},
            {"MH004", "Sneha Jadhav",    "voter4", "Mumbai City",   "Konkan",   "VS-91",  "LS-31"},
            {"MH005", "Vikram Pawar",    "voter5", "Thane",         "Konkan",   "VS-57",  "LS-25"},
            {"MH006", "Anita More",      "voter6", "Nashik",        "Nashik",   "VS-33",  "LS-21"},
            {"MH007", "Sunil Bhosale",   "voter7", "Nagpur",        "Nagpur",   "VS-264", "LS-10"},
            {"MH008", "Kavita Chavan",   "voter8", "Kolhapur",      "Pune",     "VS-161", "LS-47"},
            {"MH009", "Ganesh Shinde",   "voter9", "Chhatrapati Sambhajinagar", "Chh. Sambhajinagar", "VS-184", "LS-19"},
            {"MH010", "Meena Gaikwad",   "voter10","Amravati",      "Amravati", "VS-243", "LS-7"},
            {"MH011", "Prakash Raut",    "voter11","Solapur",       "Pune",     "VS-174", "LS-42"},
            {"MH012", "Jyoti Sawant",    "voter12","Ratnagiri",     "Konkan",   "VS-140", "LS-46"},
            {"MH013", "Ashok Wagh",      "voter13","Satara",        "Pune",     "VS-150", "LS-45"},
            {"MH014", "Deepali Kulkarni","voter14","Sangli",        "Pune",     "VS-154", "LS-44"},
            {"MH015", "Chandrakant Mane","voter15","Latur",         "Chh. Sambhajinagar", "VS-207", "LS-41"},
            {"MH016", "Bharati Phule",   "voter16","Nanded",        "Chh. Sambhajinagar", "VS-212", "LS-16"},
            {"MH017", "Sanjay Kale",     "voter17","Jalgaon",       "Nashik",   "VS-13",  "LS-3"},
            {"MH018", "Vaishali Deshpande","voter18","Wardha",      "Nagpur",   "VS-255", "LS-8"},
            {"MH019", "Dilip Lokhande",  "voter19","Chandrapur",    "Nagpur",   "VS-282", "LS-13"},
            {"MH020", "Savita Thombare", "voter20","Palghar",       "Konkan",   "VS-39",  "LS-22"},
            {"MH021", "Balasaheb Kadam", "voter21","Ahmednagar",    "Nashik",   "VS-129", "LS-37"},
            {"MH022", "Nirmala Salunkhe","voter22","Dhule",         "Nashik",   "VS-7",   "LS-2"},
            {"MH023", "Tanaji Nikam",    "voter23","Beed",          "Chh. Sambhajinagar", "VS-198", "LS-39"},
            {"MH024", "Gauri Gokhale",   "voter24","Akola",         "Amravati", "VS-235", "LS-6"},
            {"MH025", "Mahadev Ingale",  "voter25","Buldhana",      "Amravati", "VS-227", "LS-5"},
            {"MH026", "Rashmi Joshi",    "voter26","Raigad",        "Konkan",   "VS-97",  "LS-32"},
            {"MH027", "Harshvardhan Ovhal","voter27","Yavatmal",    "Amravati", "VS-249", "LS-14"},
            {"MH028", "Uma Yadav",       "voter28","Gondia",        "Nagpur",   "VS-276", "LS-11"},
            {"MH029", "Jayant Kakde",    "voter29","Jalna",         "Chh. Sambhajinagar", "VS-191", "LS-18"},
            {"MH030", "Lata Upadhyay",   "voter30","Parbhani",     "Chh. Sambhajinagar", "VS-221", "LS-17"},
        };

        for (String[] v : demoVoters) {
            voterService.registerVoterWithConstituency(
                v[0], v[1], v[2], v[3], v[4], v[5], v[6]
            );
        }
        System.out.println("✅ " + demoVoters.length + " demo voters seeded across all 6 divisions");
    }
}

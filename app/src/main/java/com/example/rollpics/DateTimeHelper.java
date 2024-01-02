package com.example.rollpics;

// DateTimeHelper.java

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeHelper {

    public static String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日 HH时mm分", Locale.getDefault());
        Date currentDate = new Date();
        return dateFormat.format(currentDate);
    }
}


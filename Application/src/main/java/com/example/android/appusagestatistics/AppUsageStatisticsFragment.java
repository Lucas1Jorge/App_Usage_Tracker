/*
* Copyright 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.appusagestatistics;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.lang.Object;
import java.util.Map;

/**
 * Fragment that demonstrates how to use App Usage Statistics API.
 */
public class AppUsageStatisticsFragment extends Fragment {

    private static final String TAG = AppUsageStatisticsFragment.class.getSimpleName();

    //VisibleForTesting for variables below
    UsageStatsManager mUsageStatsManager;
    UsageListAdapter mUsageListAdapter;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    Button mOpenUsageSettingButton;
    Spinner mSpinner;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment {@link AppUsageStatisticsFragment}.
     */
    public static AppUsageStatisticsFragment newInstance() {
        AppUsageStatisticsFragment fragment = new AppUsageStatisticsFragment();
        return fragment;
    }

    public AppUsageStatisticsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsageStatsManager = (UsageStatsManager) getActivity()
                .getSystemService("usagestats"); //Context.USAGE_STATS_SERVICE
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_usage_statistics, container, false);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        mUsageListAdapter = new UsageListAdapter();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
        mOpenUsageSettingButton = (Button) rootView.findViewById(R.id.button_open_usage_setting);
        mSpinner = (Spinner) rootView.findViewById(R.id.spinner_time_span);
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.action_list, android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(spinnerAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            String[] strings = getResources().getStringArray(R.array.action_list);

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                StatsUsageInterval statsUsageInterval = StatsUsageInterval.getValue(strings[position]);
                if (statsUsageInterval != null) {
                    List<UsageStats> usageStatsList =  getUsageStatistics(statsUsageInterval.mInterval);

                    Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());

                    HashMap<String, Integer> m = new HashMap<String, Integer>();
                    HashMap<String, UsageStats> mapUsageStats = new HashMap<String, UsageStats>();

                    boolean validPeriod = true;
//                    for (int i = 0; validPeriod == true; i++) {
//                        Log.d(TAG, "Pkg: " + getAppNameFromPackage(u.getPackageName(), getContext()) +  "\t" + "ForegroundTime: "
//                        + u.getTotalTimeInForeground() + " milliseconds") ;
//                        System.out.println(usageStatsList.get(i).getLastTimeUsed());

                    for (int i = 0; i < usageStatsList.size(); i++) {

                        String name = usageStatsList.get(i).getPackageName();

                        if (name != null) {
                            if (!m.containsKey(name)) {
                                m.put(name, 1);
                                mapUsageStats.put(name, usageStatsList.get(i));
                            } else {
                                m.put(name, m.get(name) + 1);
                            }
                        }

                    }

//                        if (statsUsageInterval.mStringRepresentation.equals("Daily"))
//                            validPeriod = checkSameDay(usageStatsList.get(0).getLastTimeUsed(), usageStatsList.get(i).getLastTimeUsed());
//                        else if (statsUsageInterval.mStringRepresentation.equals("Weekly"))
//                            validPeriod = checkSameWeek(usageStatsList.get(0).getLastTimeUsed(), usageStatsList.get(i).getLastTimeUsed());
//                        else if (statsUsageInterval.mStringRepresentation.equals("Monthly"))
//                            validPeriod = checkSameMonth(usageStatsList.get(0).getLastTimeUsed(), usageStatsList.get(i).getLastTimeUsed());
//                        else if (statsUsageInterval.mStringRepresentation.equals("Yearly"))
//                            validPeriod = checkSameYear(usageStatsList.get(0).getLastTimeUsed(), usageStatsList.get(i).getLastTimeUsed());
//                        if (i == 200) validPeriod = false;
//                    }

                    printForegroundTime(usageStatsList);

                    printMap(m);
                    System.out.println("\n********************* Now the sorted version\n");


                    List<UsageStats> sortedUsageStatsList = sortMapByValue(m, mapUsageStats);
//                    printList(sortedUsageStatsList);
                    sortedUsageStatsList = filterList(sortedUsageStatsList, getContext());

                    updateAppsList(sortedUsageStatsList, m);

                    System.out.println(statsUsageInterval.mStringRepresentation);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public static String getAppNameFromPackage(String packageName, Context context) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> pkgAppsList = context.getPackageManager()
                .queryIntentActivities(mainIntent, 0);

        for (ResolveInfo app : pkgAppsList) {
            if (app.activityInfo.packageName.equals(packageName)) {
                return app.activityInfo.loadLabel(context.getPackageManager()).toString();
            }
        }
        return null;
    }

    // Debugging functions

    public static void printMap(Map<String, Integer> map) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    public static void printList(List<UsageStats> list) {
        for (UsageStats u : list) {
            System.out.println(u.getPackageName());
        }
    }

    public static void printForegroundTime(List<UsageStats> usageStatsList) {
        for (UsageStats u : usageStatsList) {
            Log.d(TAG, "Pkg: " + u.getPackageName() + "\t" + "ForegroundTime: "
                    + u.getTotalTimeInForeground() + " milliseconds");
        }
    }

//    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
    public static List<UsageStats> sortMapByValue(HashMap<String, Integer> map, HashMap<String, UsageStats> mapUsageStats) {
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(list, new mapComparator());

        System.out.println("*************************** Printing ordered list:");
        for (HashMap.Entry<String, Integer> el : list) {
            System.out.println(el.getKey() + ": " + el.getValue());
        }

        List<UsageStats> ans = new ArrayList<UsageStats>();
        for (HashMap.Entry<String, Integer> entry : list) {
            ans.add(mapUsageStats.get(entry.getKey()));
        }

        return ans;
    }

    public List<UsageStats> filterList(List<UsageStats> list, Context context) {
        List<UsageStats> ans = new ArrayList<UsageStats>();

        for (UsageStats u : list) {
            if (getAppNameFromPackage(u.getPackageName(), context) != null) {
                ans.add(u);
            }
        }

        return ans;
    }

    public boolean checkSameDay(Long day1, Long day2) {
        DateFormat mDateFormat = new SimpleDateFormat();
        String date1 = mDateFormat.format(new Date(day1));
        String date2 = mDateFormat.format(new Date(day2));

//        String[] parts1 = date1.split();
//        String[] parts2 = date2.split();
        System.out.println(date1);
        return true;
    }

    public boolean checkSameWeek(Long week1, Long week2) {
        DateFormat mDateFormat = new SimpleDateFormat();
        Date date1 = new Date(week1);
        Date date2 = new Date(week2);
        return true;
    }

    public boolean checkSameMonth(Long month1, Long month2) {
        DateFormat mDateFormat = new SimpleDateFormat();
        Date date1 = new Date(month1);
        Date date2 = new Date(month2);
        return true;
    }

    public boolean checkSameYear(Long year1, Long year2) {
        DateFormat mDateFormat = new SimpleDateFormat();
        Date date1 = new Date(year1);
        Date date2 = new Date(year2);
        return true;
    }

//    public static void printMap(HashMap mp) {
//        Iterator it = mp.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry pair = (Map.Entry)it.next();
//            System.out.println(pair.getKey() + " = " + pair.getValue());
//            it.remove(); // avoids a ConcurrentModificationException
//        }
//    }

    /**
     * Returns the {@link #mRecyclerView} including the time span specified by the
     * intervalType argument.
     *
     * @param intervalType The time interval by which the stats are aggregated.
     *                     Corresponding to the value of {@link UsageStatsManager}.
     *                     E.g. {@link UsageStatsManager#INTERVAL_DAILY}, {@link
     *                     UsageStatsManager#INTERVAL_WEEKLY},
     *
     * @return A list of {@link android.app.usage.UsageStats}.
     */
    public List<UsageStats> getUsageStatistics(int intervalType) {
        // Get the app statistics since one year ago from the current time.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);

        List<UsageStats> queryUsageStats = mUsageStatsManager
                .queryUsageStats(intervalType, cal.getTimeInMillis(),
                        System.currentTimeMillis());

        if (queryUsageStats.size() == 0) {
            Log.i(TAG, "The user may not allow the access to apps usage. ");
            Toast.makeText(getActivity(),
                    getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();
            mOpenUsageSettingButton.setVisibility(View.VISIBLE);
            mOpenUsageSettingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                }
            });
        }
        return queryUsageStats;
    }

    /**
     * Updates the {@link #mRecyclerView} with the list of {@link UsageStats} passed as an argument.
     *
     * @param usageStatsList A list of {@link UsageStats} from which update the
     *                       {@link #mRecyclerView}.
     */
    //VisibleForTesting
    void updateAppsList(List<UsageStats> usageStatsList, HashMap<String, Integer> m) {
        List<CustomUsageStats> customUsageStatsList = new ArrayList<>();
        List<Integer> usesCount = new ArrayList<Integer>();
        for (int i = 0; i < usageStatsList.size(); i++) {
            CustomUsageStats customUsageStats = new CustomUsageStats();
            customUsageStats.usageStats = usageStatsList.get(i);
            try {
                Drawable appIcon = getActivity().getPackageManager()
                        .getApplicationIcon(customUsageStats.usageStats.getPackageName());
                customUsageStats.appIcon = appIcon;
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, String.format("App Icon is not found for %s",
                        customUsageStats.usageStats.getPackageName()));
                customUsageStats.appIcon = getActivity()
                        .getDrawable(R.drawable.ic_default_app_launcher);
            }
            customUsageStatsList.add(customUsageStats);
            usesCount.add(m.get(usageStatsList.get(i).getPackageName()));
        }
        mUsageListAdapter.setCustomUsageStatsList(customUsageStatsList);
        mUsageListAdapter.setUsesCount(usesCount);
        mUsageListAdapter.notifyDataSetChanged();
        mUsageListAdapter.setContext(getContext());
        mRecyclerView.scrollToPosition(0);
    }

    /**
     * The {@link Comparator} to sort a collection of {@link UsageStats} sorted by the timestamp
     * last time the app was used in the descendant order.
     */
    private static class LastTimeLaunchedComparatorDesc implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats left, UsageStats right) {
            return Long.compare(right.getLastTimeUsed(), left.getLastTimeUsed());
        }
    }

    private static class mapComparator implements Comparator<HashMap.Entry<String, Integer>> {

        @Override
        public int compare(HashMap.Entry<String, Integer> left, HashMap.Entry<String, Integer> right) {
            return Long.compare(right.getValue(), left.getValue());
        }
    }

    /**
     * Enum represents the intervals for {@link android.app.usage.UsageStatsManager} so that
     * values for intervals can be found by a String representation.
     *
     */
    //VisibleForTesting
    static enum StatsUsageInterval {
        DAILY("Daily", UsageStatsManager.INTERVAL_DAILY),
        WEEKLY("Weekly", UsageStatsManager.INTERVAL_WEEKLY),
        MONTHLY("Monthly", UsageStatsManager.INTERVAL_MONTHLY),
        YEARLY("Yearly", UsageStatsManager.INTERVAL_YEARLY);

        private int mInterval;
        private String mStringRepresentation;

        StatsUsageInterval(String stringRepresentation, int interval) {
            mStringRepresentation = stringRepresentation;
            mInterval = interval;
        }

        static StatsUsageInterval getValue(String stringRepresentation) {
            for (StatsUsageInterval statsUsageInterval : values()) {
                if (statsUsageInterval.mStringRepresentation.equals(stringRepresentation)) {
                    return statsUsageInterval;
                }
            }
            return null;
        }
    }
}

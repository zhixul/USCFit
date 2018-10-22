package com.example.fred.uscfit;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.Activity;
import com.example.Footstep;
import com.example.Plan;
import com.example.db.DBController;
import com.google.api.Distribution;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {
    private static final String TAG = "Progress";
    private DBController dbController = null;
    private String mEmail;
    private Map<String, Plan> myPlans;
    private Map<String, List<Activity>> myActivities;
    private Map<String, Footstep> myFootsteps;
    private Calendar cal;
    private SimpleDateFormat sdf;

    private ConstraintLayout mConstraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        Intent intent = getIntent();
        mEmail = intent.getStringExtra("email");
        dbController = new DBController();
        cal = Calendar.getInstance();
        sdf = new SimpleDateFormat("yyyy_MM_dd");
        mConstraintLayout = (ConstraintLayout) findViewById(R.id.constraintLayout);


        GetAllPlansTask mGetAllPlansTask = new GetAllPlansTask((mEmail));
        mGetAllPlansTask.execute((Void) null);

    }

    public void updatePlanStatus() {
        SimpleDateFormat outputSdf = new SimpleDateFormat("yyyy MMM.dd");
        boolean weeklyPlanCompleted = true;
        for(int i=0; i<mConstraintLayout.getChildCount(); i++) {
            Calendar currCal = cal;
            currCal.add(Calendar.DAY_OF_MONTH, i * (-1));
            String currDate = sdf.format(currCal.getTime());
            String outputDate = outputSdf.format(currCal.getTime());

            if(mConstraintLayout.getChildAt(i).getClass() != CardView.class) {
                continue;
            }
            CardView childCardView = (CardView) mConstraintLayout.getChildAt(i);
            LinearLayout childLinearLayout = (LinearLayout) childCardView.getChildAt(0);
            ProgressBar childProgressBar = (ProgressBar) childLinearLayout.getChildAt(0);
            LinearLayout childVerticalLayout = (LinearLayout) childLinearLayout.getChildAt(1);
            final ImageView childImageView = (ImageView) childVerticalLayout.getChildAt(0);
            TextView childTextView = (TextView) childVerticalLayout.getChildAt(1);

            // get plan
            Plan plan = myPlans.get(currDate);
            if(plan == null) {
                childProgressBar.setProgress(100);
                childTextView.setText("No plan for " + outputDate);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        childImageView.setVisibility(View.GONE);
                    }
                });
                weeklyPlanCompleted = false;
                continue;
            }

            // get footsteps
            long actualStep = 0;
            if(myFootsteps.containsKey(currDate)) {
                actualStep = myFootsteps.get(currDate).value;
            }
            Footstep targetFootstep = null;
            long targetStep = actualStep;
            for(Object o: plan.activity) {
                if(o.getClass() == Footstep.class) {
                    targetFootstep = (Footstep) o;
                    break;
                }
            }
            if(targetFootstep != null) {
                targetStep = targetFootstep.value;
            }
            double footstepProgress = (double) actualStep/targetStep * 100;
            childProgressBar.setProgress((int)footstepProgress);

            // get plan details
            boolean plancompleted = true;
            for(Object o: plan.activity) {
                if(o.getClass() == Activity.class) {
                    Activity plannedActivity = (Activity) o;
                    List<Activity> currActivities = myActivities.get(currDate);
                    boolean plannedActivityCompleted = false;
                    for(Activity activity : currActivities) {
                        if(isFinishedActivity(activity, plannedActivity)) {
                            plannedActivityCompleted = true;
                            break;
                        }
                    }
                    if(!plannedActivityCompleted) {
                        plancompleted = false;
                        break;
                    }
                }
            }
            if(plancompleted) {
                childTextView.setText(outputDate + " Completed");
            }
            else {
                childTextView.setText(outputDate + " Not completed");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        childImageView.setVisibility(View.GONE);
                    }
                });
                weeklyPlanCompleted = false;
            }
        }

        if(!weeklyPlanCompleted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImageView weeklyBadgeView = (ImageView) findViewById(R.id.weeklyBadge);
                    weeklyBadgeView.setVisibility(View.GONE);
                }
            });
        }
    }


    // return true is a fulfills plan b
    public boolean isFinishedActivity(Activity a, Activity b) {
        if(!a.name.equals(b.name)) return false;
//        if a start later than b
        if(a.start.compareTo(b.start) > 0) return false;
//        if a ends earlier than b
        if(a.end.compareTo(b.end) < 0) return false;
        return true;
    }




    public class GetAllPlansTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;

        public GetAllPlansTask(String email) {
            mEmail = email;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Map<String, Plan> myPlans = new HashMap<>();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
            for(int i = 0; i<3; i++) {
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                String planName = sdf.format(calendar.getTime());
                Log.d(TAG, "doInBackground: planName" + planName );
                Plan currPlan = dbController.getPlan(mEmail, planName);
                if(currPlan != null) {
                    myPlans.put(planName, currPlan);
                }
            }
            Log.d(TAG, "doInBackground: " + myPlans.size());

            List<Object> allActivities = dbController.getAllActivity(mEmail);
            Map<String, List<Activity>> myActivities = new HashMap<>();
            Map<String, Footstep> myFootsteps = new HashMap<>();
            for(Object o: allActivities) {
                HashMap<String, Object> map = (HashMap<String, Object>) o;
                if(map.get("name").equals("footsteps")) {
                    Footstep footstep = new Footstep();
                    footstep.name = "footsteps";
                    footstep.date = (Timestamp) map.get("date");
                    footstep.value = (Long) map.get("value");
                    String currDate = sdf.format(footstep.date.toDate());
                    myFootsteps.put(currDate, footstep);
                }
                else {
                    Activity activity = new Activity();
                    activity.name = (String) map.get("name");
                    activity.start = (Timestamp)map.get("start");
                    activity.end = (Timestamp)map.get("end");
                    String currDate = sdf.format(activity.start.toDate());
                    if(myActivities.containsKey(currDate)) {
                        myActivities.get(currDate).add(activity);
                    }
                    else {
                        List<Activity> newActivity = new ArrayList<>();
                        newActivity.add(activity);
                        myActivities.put(currDate, newActivity);
                    }
                }
            }

            ProgressActivity.this.myActivities = myActivities;
            ProgressActivity.this.myPlans = myPlans;
            ProgressActivity.this.myFootsteps = myFootsteps;

//            ProgressActivity.this.updateFootstepBar();
            ProgressActivity.this.updatePlanStatus();


            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                // this means sport already exists
//                alert("Oops...", "Sport category already exists.");
//                ProgressActivity.this.updateFootstepBar();
                return;
            } else {
                // sport doesn't exist
                // all input is valid, now call DBController to insert sport
//                dbController.addSports(getIntent().getStringExtra("email"), new Sport(sportName, calories));
//                alert("Yeah!", "Successfully added sport category.");
                return;
            }
        }


    }
}

package com.example.android.wearable.wear.wearnotifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.*
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.android.wearable.wear.common.mock.MockDatabase
import com.example.android.wearable.wear.common.util.NotificationUtil
import com.example.android.wearable.wear.wearnotifications.handlers.*
import com.google.android.material.snackbar.Snackbar
import java.util.*

/**
 * The Activity demonstrates several popular Notification.Style examples along with their best
 * practices (include proper Wear support when you don't have a dedicated Wear app).
 */
class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var mNotificationManagerCompat: NotificationManagerCompat? = null

    private var mSelectedNotification = 0

    // RelativeLayout required for SnackBars to alert users when Notifications are disabled for app.
    private var mMainRelativeLayout: RelativeLayout? = null
    private var mSpinner: Spinner? = null
    private var mNotificationDetailsTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMainRelativeLayout = findViewById<View>(R.id.mainRelativeLayout) as RelativeLayout
        mNotificationDetailsTextView = findViewById<View>(R.id.notificationDetails) as TextView
        mSpinner = findViewById<View>(R.id.spinner) as Spinner

        mNotificationManagerCompat = NotificationManagerCompat.from(applicationContext)

        // Create an ArrayAdapter using the string array and a default spinner layout.
        val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                NOTIFICATION_STYLES)
        // Specify the layout to use when the list of choices appears.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner.
        mSpinner!!.adapter = adapter
        mSpinner!!.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected(): position: $position id: $id")

        mSelectedNotification = position

        mNotificationDetailsTextView!!.text = NOTIFICATION_STYLES_DESCRIPTION[mSelectedNotification]
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Required
    }

    fun onClick(view: View) {

        Log.d(TAG, "onClick()")

        val areNotificationsEnabled = mNotificationManagerCompat!!.areNotificationsEnabled()

        if (!areNotificationsEnabled) {
            // Because the user took an action to create a notification, we create a prompt to let
            // the user re-enable notifications for this application again.
            val snackbar = Snackbar
                    .make(
                            mMainRelativeLayout!!,
                            "You need to enable notifications for this app",
                            Snackbar.LENGTH_LONG)
                    .setAction("ENABLE") {
                        // Links to this app's notification settings
                        openNotificationSettingsForApp()
                    }
            snackbar.show()
            return
        }

        when (NOTIFICATION_STYLES[mSelectedNotification]) {
            BIG_TEXT_STYLE -> generateBigTextStyleNotification()

            BIG_PICTURE_STYLE -> generateBigPictureStyleNotification()

            INBOX_STYLE -> generateInboxStyleNotification()

            MESSAGING_STYLE -> generateMessagingStyleNotification()

            MEDIA_STYLE -> generateMediaStyleNotification()

            PROGRESS_STYLE -> generateProgressTemplate()

            BUNDLED_NOTIFICATION -> generateBundledNotification()

            SMART_NOTIFICATION -> generateSmartReply()

            NOTIFICATION_BUBBLE -> generateNotificationBubble()

        }
    }

    @SuppressLint("NewApi")
    private fun generateNotificationBubble() {

        val bubbleMetadata = androidx.core.app.NotificationCompat.BubbleMetadata.Builder()
                // The height of the expanded bubble.
                .setDesiredHeight(this.resources.getDimensionPixelSize(R.dimen.bubble_height))

                // The icon of the bubble.
                // TODO: The icon is not displayed in Android Q Beta 2.
                .setIcon(IconCompat.createWithAdaptiveBitmap(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.wendy_wonda)
                ))
                .apply {
                    // When the bubble is explicitly opened by the user, we can show the bubble automatically
                    // in the expanded state. This works only when the app is in the foreground.
                    // TODO: This does not yet work in Android Q Beta 2.

                        setAutoExpandBubble(false)
                        setSuppressNotification(false)

//                        setSuppressInitialNotification(true)
                }
                // The Intent to be used for the expanded bubble.
                .setIntent(
                        PendingIntent.getActivity(
                                this,
                                1,
                                // Launch BubbleActivity as the expanded bubble.
                                Intent(this, BigPictureSocialMainActivity::class.java)
                                        .setAction(Intent.ACTION_VIEW),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                ).build()



        val reminderAppData = MockDatabase.getBigPictureStyleData()

        val notificationChannelId = NotificationUtil.createNotificationChannel(this, reminderAppData)

        val notificationCompatBuilder = NotificationCompat.Builder(
                applicationContext, notificationChannelId)

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder)

        // Create notification
        val chatBot = Person.Builder()
                .setBot(true)
                .setName("BubbleBot")
                .setImportant(true)
                .build().uri

        val notification = notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content for API 16 (4.1 and after).
                // Title for API <16 (4.0 and below) devices.
                .setContentTitle("Title")
                .setColorized(true)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
                .setStyle(BigTextStyle().bigText("").setBigContentTitle("how does this recipe looks"))
                // Content for API <24 (7.0 and below) devices.
                .setContentText("What are you cooking tonight? any suggestions")
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.alarm))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))

                .setBubbleMetadata(bubbleMetadata)
                .addPerson(chatBot)
                .setCategory(Notification.CATEGORY_MESSAGE)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(NotificationCompat.PRIORITY_HIGH)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(reminderAppData.channelLockscreenVisibility)

                // Adds additional actions specified above.
                .setAllowSystemGeneratedContextualActions(true)

                .build()

        mNotificationManagerCompat!!.notify(NOTIFICATION_ID, notification)

    }

    // Android 10 only
    private fun generateSmartReply() {

            val bigTextStyleReminderAppData = MockDatabase.getBigTextStyleData()

            val notificationChannelId = NotificationUtil.createNotificationChannel(this, bigTextStyleReminderAppData)


            // Create the RemoteInput.
            val replyLabel = getString(R.string.reply_label)
            val remoteInput = RemoteInput.Builder(BigPictureSocialIntentService.EXTRA_COMMENT)
                    .setLabel(replyLabel)
                    // List of quick response choices for any wearables paired with the phone
//                .setChoices(bigPictureStyleSocialAppData.possiblePostResponses)
                    .build()

            // Pending intent =
            //      API <24 (M and below): activity so the lock-screen presents the auth challenge
            //      API 24+ (N and above): this should be a Service or BroadcastReceiver
            val replyActionPendingIntent: PendingIntent

            val mainIntent = Intent(this, BigPictureSocialMainActivity::class.java)

            val mainPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val intent = Intent(this, BigPictureSocialIntentService::class.java)
                intent.action = BigPictureSocialIntentService.ACTION_COMMENT
                replyActionPendingIntent = PendingIntent.getService(this, 0, intent, 0)

            } else {
                replyActionPendingIntent = mainPendingIntent
            }

            val replyAction = NotificationCompat.Action.Builder(
                    R.drawable.ic_reply_white_18dp,
                    replyLabel,
                    replyActionPendingIntent)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .build()


            // 4. Create additional Actions (Intents) for the Notification.

            // In our case, we create two additional actions: a Snooze action and a Dismiss action.
            // Snooze Action.
            val snoozeIntent = Intent(this, BigTextIntentService::class.java)
            snoozeIntent.action = BigTextIntentService.ACTION_SNOOZE


            val notificationCompatBuilder = NotificationCompat.Builder(
                    applicationContext, notificationChannelId)

            GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder)

            val notification = notificationCompatBuilder
                    // BIG_TEXT_STYLE sets title and content for API 16 (4.1 and after).
                    // Title for API <16 (4.0 and below) devices.
                    .setContentTitle("Title")
                    .setColorized(true)
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
                    .setStyle(BigTextStyle().bigText("").setBigContentTitle("how does this recipe looks"))
                    // Content for API <24 (7.0 and below) devices.
                    .setContentText("What are you cooking tonight? any suggestions")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(
                            resources,
                            R.drawable.alarm))
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    // Set primary color (important for Wear 2.0 Notifications).
                    .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))


                    .setCategory(Notification.CATEGORY_MESSAGE)

                    // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                    // 'importance' which is set in the NotificationChannel. The integers representing
                    // 'priority' are different from 'importance', so make sure you don't mix them.
                    .setPriority(bigTextStyleReminderAppData.priority)

                    // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                    // visibility is set in the NotificationChannel.
                    .setVisibility(bigTextStyleReminderAppData.channelLockscreenVisibility)

                    // Adds additional actions specified above.
                    .addAction(replyAction)
                    .setAllowSystemGeneratedContextualActions(true)

                    .build()

            mNotificationManagerCompat!!.notify(NOTIFICATION_ID, notification)
    }

    private fun generateBundledNotification() {
        val mediaStyleData = MockDatabase.getMediaStyleData()
        val notificationChannelId = NotificationUtil.createNotificationChannel(this, mediaStyleData)

        val builder1 = NotificationCompat.Builder(this, notificationChannelId).apply {

            setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(
                            resources,
                            R.drawable.ic_person_black_48dp))
                    .priority = NotificationCompat.PRIORITY_LOW
            color = ContextCompat.getColor(applicationContext, R.color.colorPrimary)

        }

        val builder = NotificationCompat.Builder(this, notificationChannelId).apply {
            setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("title")
                    .setContentText("text")
        }

        NotificationManagerCompat.from(this).apply {

            val groupKey = "key"

            for (i in 0..5) {
                builder.setContentTitle("Notification :$i")
                builder.setContentText("Notification content :$i")
                builder.setGroup(groupKey)
                builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                notify(Random().nextInt(100), builder.build())
            }
        }

        //   Summary is the only notification that appears on
        //   Marshmallow and lower devices and should (you guessed it)
        //   summarize all of the individual notifications.
        //   This is an opportune time to use the InboxStyle,
        //   although using it is not a requirement.
        //   On Android N and higher devices, some information
        //   (such as the subtext, content intent, and delete intent)
        //   is extracted from the summary notification to produce the
        //   collapsed notification for the bundled notifications so you
        //   should continue to generate a summary notification on all API levels.
        NotificationManagerCompat.from(this).apply {

            val groupKey = "key"

            val summaryId = (groupKey.hashCode())

            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(
                            resources,
                            R.drawable.ic_person_black_48dp))
                    .priority = NotificationCompat.PRIORITY_LOW
            builder.color = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
            builder.setContentTitle("Summary Title")
            builder.setContentText("Summary Content")
            builder.setGroup(groupKey)
            builder.setGroupSummary(true)
            builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            builder.setStyle(InboxStyle())
            notify(summaryId, builder.build())


        }

    }

    private fun generateProgressTemplate() {
        val mediaStyleData = MockDatabase.getMediaStyleData()
        val notificationChannelId = NotificationUtil.createNotificationChannel(this, mediaStyleData)

        val builder = NotificationCompat.Builder(this,
                notificationChannelId).apply {
            setContentTitle("Picture Download")
            setContentText("Download in progress")
            setSmallIcon(R.drawable.ic_launcher)
            setPriority(NotificationCompat.PRIORITY_LOW)
        }
        val PROGRESS_MAX = 100
        val PROGRESS_CURRENT = 0
        NotificationManagerCompat.from(this).apply {
            // Issue the initial notification with zero progress
            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
            notify(NOTIFICATION_ID, builder.build())

            // Do the job here that tracks the progress.
            // Usually, this should be in a
            // worker thread
            // To show progress, update PROGRESS_CURRENT and update the notification with:
            // builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
            // notificationManager.notify(notificationId, builder.build());

            for (i in 0..100) {
                builder.setProgress(100, i, false)

                notify(NOTIFICATION_ID, builder.build())
            }
            // When done, update the notification one more time to remove the progress bar
            builder.setContentText("Download complete")
                    .setProgress(0, 0, false)
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun generateMediaStyleNotification() {

        val mediaSessionCompat = MediaSessionCompat(this, "tag")
        mediaSessionCompat.isActive = true
        val customActions = ArrayList<PlaybackState.CustomAction>()
        //        PlaybackStateCompat state = new PlaybackStateCompat(1,1L,4,5, 4,3,"34",434, customActions,3,null);
        mediaSessionCompat.setPlaybackState(PlaybackStateCompat.fromPlaybackState(null))
        // 0. Get your data (everything unique per Notification).
        val mediaStyleData = MockDatabase.getMediaStyleData()

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId = NotificationUtil.createNotificationChannel(this, mediaStyleData)


        // 2. Build the BIG_TEXT_STYLE.
        var mediaStyle: androidx.media.app.NotificationCompat.MediaStyle? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            mediaStyle.setShowCancelButton(true)
            mediaStyle.setShowActionsInCompactView(2, 3, 4)
            mediaStyle.setMediaSession(mediaSessionCompat.sessionToken) // to use colorized option
        }


        // 3. Set up main Intent for notification.
        val notifyIntent = Intent(this, BigTextMainActivity::class.java)


        // Sets the Activity to start in a new, empty task
        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val notifyPendingIntent = PendingIntent.getActivity(
                this,
                0,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )


        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(
                applicationContext, notificationChannelId)

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder)

        val notification = notificationCompatBuilder
                .setStyle(mediaStyle)
                .setContentTitle(mediaStyleData.contentTitle)
                .setContentText(mediaStyleData.contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setColorized(true)
                .setColor(ContextCompat.getColor(applicationContext, R.color.md_red_700))
                .setLargeIcon(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.top_boy))
                .setContentIntent(notifyPendingIntent)
                .addAction(R.drawable.ic_music_note_black_24dp, "not happy", null)
                .addAction(R.drawable.ic_notifications_paused_black_24dp, "no", null)
                .addAction(R.drawable.ic_skip_previous_black_24dp, "no", null)
                .addAction(R.drawable.ic_pause_black_24dp, "no", null)
                .addAction(R.drawable.ic_skip_next_black_24dp, "no", null)
                .setSubText("Song name")
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                .setCategory(Notification.CATEGORY_REMINDER)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(mediaStyleData.priority)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(mediaStyleData.channelLockscreenVisibility)

                // Adds additional actions specified above.

                .build()

        mNotificationManagerCompat!!.notify(NOTIFICATION_ID, notification)

    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that supports both phone/tablet and wear. For devices
     * on API level 16 (4.1.x - Jelly Bean) and after, displays BIG_TEXT_STYLE. Otherwise, displays
     * a basic notification.
     */
    private fun generateBigTextStyleNotification() {

        Log.d(TAG, "generateBigTextStyleNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up main Intent for notification
        //      4. Create additional Actions for the Notification
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        val bigTextStyleReminderAppData = MockDatabase.getBigTextStyleData()

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId = NotificationUtil.createNotificationChannel(this, bigTextStyleReminderAppData)


        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
                // Overrides ContentText in the big form of the template.
                .bigText(bigTextStyleReminderAppData.bigText)
                // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(bigTextStyleReminderAppData.bigContentTitle)
                // Summary line after the detail section in the big form of the template.
                // Note: To improve readability, don't overload the user with info. If Summary Text
                // doesn't add critical information, you should skip it.
                .setSummaryText(bigTextStyleReminderAppData.summaryText)


        // 3. Set up main Intent for notification.
        val notifyIntent = Intent(this, BigTextMainActivity::class.java)
        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val notifyPendingIntent = PendingIntent.getActivity(
                this,
                0,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Snooze Action.
        val snoozeIntent = Intent(this, BigTextIntentService::class.java)
        snoozeIntent.action = BigTextIntentService.ACTION_SNOOZE

        val snoozePendingIntent = PendingIntent.getService(this, 0, snoozeIntent, 0)
        val snoozeAction = NotificationCompat.Action.Builder(
                R.drawable.ic_alarm_white_48dp,
                "Snooze",
                snoozePendingIntent)
                .build()


        // Dismiss Action.
        val dismissIntent = Intent(this, BigTextIntentService::class.java)
        dismissIntent.action = BigTextIntentService.ACTION_DISMISS

        val dismissPendingIntent = PendingIntent.getService(this, 0, dismissIntent, 0)
        val dismissAction = NotificationCompat.Action.Builder(
                R.drawable.ic_cancel_white_48dp,
                "Dismiss",
                dismissPendingIntent)
                .build()


        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(
                applicationContext, notificationChannelId)

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder)

        val notification = notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content for API 16 (4.1 and after).
//                .setStyle(bigTextStyle)
                // Title for API <16 (4.0 and below) devices.
                .setContentTitle(bigTextStyleReminderAppData.contentTitle)
                // Content for API <24 (7.0 and below) devices.
                .setContentText(bigTextStyleReminderAppData.contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.alarm))
                .setContentIntent(notifyPendingIntent)
                .setOngoing(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                .setCategory(Notification.CATEGORY_REMINDER)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(bigTextStyleReminderAppData.priority)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(bigTextStyleReminderAppData.channelLockscreenVisibility)

                // Adds additional actions specified above.
                .addAction(snoozeAction)
                .addAction(dismissAction)

                .build()

        mNotificationManagerCompat!!.notify(NOTIFICATION_ID, notification)
    }

    /*
     * Generates a BIG_PICTURE_STYLE Notification that supports both phone/tablet and wear. For
     * devices on API level 16 (4.1.x - Jelly Bean) and after, displays BIG_PICTURE_STYLE.
     * Otherwise, displays a basic notification.
     *
     * This example Notification is a social post. It allows updating the notification with
     * comments/responses via RemoteInput and the BigPictureSocialIntentService on 24+ (N+) and
     * Wear devices.
     */
    private fun generateBigPictureStyleNotification() {

        Log.d(TAG, "generateBigPictureStyleNotification()")


        // 0. Get your data (everything unique per Notification).
        val bigPictureStyleSocialAppData = MockDatabase.getBigPictureStyleData()

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId = NotificationUtil.createNotificationChannel(this, bigPictureStyleSocialAppData)

        // 2. Build the BIG_PICTURE_STYLE.
        val bigPictureStyle = NotificationCompat.BigPictureStyle()
                // Provides the bitmap for the BigPicture notification.
                .bigPicture(
                        BitmapFactory.decodeResource(
                                resources,
                                bigPictureStyleSocialAppData.bigImage))
                // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(bigPictureStyleSocialAppData.bigContentTitle)
                // Summary line after the detail section in the big form of the template.
                .setSummaryText(bigPictureStyleSocialAppData.summaryText)

        // 3. Set up main Intent for notification.
        val mainIntent = Intent(this, BigPictureSocialMainActivity::class.java)

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // Even though this sample's MainActivity doesn't link to the Activity this Notification
        // launches directly, i.e., it isn't part of the normal workflow, a social app generally
        // always links to individual posts as part of the app flow, so we will follow option 1.

        // For an example of option 2, check out the BIG_TEXT_STYLE example.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        val stackBuilder = TaskStackBuilder.create(this)
        // Adds the back stack.
        stackBuilder.addParentStack(BigPictureSocialMainActivity::class.java!!)
        // Adds the Intent to the top of the stack.
        stackBuilder.addNextIntent(mainIntent)
        // Gets a PendingIntent containing the entire back stack.
        val mainPendingIntent = PendingIntent.getActivity(
                this,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 4. Set up RemoteInput, so users can input (keyboard and voice) from notification.

        // Note: For API <24 (M and below) we need to use an Activity, so the lock-screen presents
        // the auth challenge. For API 24+ (N and above), we use a Service (could be a
        // BroadcastReceiver), so the user can input from Notification or lock-screen (they have
        // choice to allow) without leaving the notification.

        // Create the RemoteInput.
        val replyLabel = getString(R.string.reply_label)
        val remoteInput = RemoteInput.Builder(BigPictureSocialIntentService.EXTRA_COMMENT)
                .setLabel(replyLabel)
                // List of quick response choices for any wearables paired with the phone
                .setChoices(bigPictureStyleSocialAppData.possiblePostResponses)
                .build()

        // Pending intent =
        //      API <24 (M and below): activity so the lock-screen presents the auth challenge
        //      API 24+ (N and above): this should be a Service or BroadcastReceiver
        val pendingIntent: PendingIntent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(this, BigPictureSocialIntentService::class.java)
            intent.action = BigPictureSocialIntentService.ACTION_COMMENT
            pendingIntent = PendingIntent.getService(this, 0, intent, 0)

        } else {
            pendingIntent = mainPendingIntent
        }

        val replyAction: Action = NotificationCompat.Action.Builder(
                R.drawable.ic_reply_white_18dp, replyLabel, pendingIntent)
                .addRemoteInput(remoteInput)
                .build()

        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for a comment on the post.

        val notificationCompatBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder)

        notificationCompatBuilder
                // BIG_PICTURE_STYLE sets title and content for API 16 (4.1 and after).
                .setStyle(bigPictureStyle)
                // Title for API <16 (4.0 and below) devices.
                .setContentTitle(bigPictureStyleSocialAppData.contentTitle)
                // Content for API <24 (7.0 and below) devices.
                .setContentText(bigPictureStyleSocialAppData.contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                //                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.md_red_700))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColorized(true)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                .setSubText(Integer.toString(1))
                .addAction(replyAction)
                .setCategory(Notification.CATEGORY_SOCIAL)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(bigPictureStyleSocialAppData.priority)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(bigPictureStyleSocialAppData.channelLockscreenVisibility)

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (name in bigPictureStyleSocialAppData.participants) {
            notificationCompatBuilder.addPerson(name)
        }

        val notification = notificationCompatBuilder.build()

        mNotificationManagerCompat!!.notify(NOTIFICATION_ID, notification)
    }

    /*
     * Generates a INBOX_STYLE Notification that supports both phone/tablet and wear. For devices
     * on API level 16 (4.1.x - Jelly Bean) and after, displays INBOX_STYLE. Otherwise, displays a
     * basic notification.
     */
    private fun generateInboxStyleNotification() {

        Log.d(TAG, "generateInboxStyleNotification()")


        val inboxStyleEmailAppData = MockDatabase.getInboxStyleData()

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId = NotificationUtil.createNotificationChannel(this, inboxStyleEmailAppData)

        // 2. Build the INBOX_STYLE.
        val inboxStyle = InboxStyle()
                // This title is slightly different than regular title, since I know INBOX_STYLE is
                // available.
                .setBigContentTitle(inboxStyleEmailAppData.bigContentTitle)
                .setSummaryText(inboxStyleEmailAppData.summaryText)

        // Add each summary line of the new emails, you can add up to 5.
        for (summary in inboxStyleEmailAppData.individualEmailSummary) {
            inboxStyle.addLine(summary)
        }

        // 3. Set up main Intent for notification.
        val mainIntent = Intent(this, InboxMainActivity::class.java)

        val stackBuilder = TaskStackBuilder.create(this)
        // Adds the back stack.
        stackBuilder.addParentStack(InboxMainActivity::class.java)
        // Adds the Intent to the top of the stack.
        stackBuilder.addNextIntent(mainIntent)
        // Gets a PendingIntent containing the entire back stack.
        val mainPendingIntent = PendingIntent.getActivity(
                this,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 4. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. However, we don't need to update this notification later, so we
        // will not need to set a global builder for access to the notification later.

        val notificationCompatBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder)

        notificationCompatBuilder

                // INBOX_STYLE sets title and content for API 16+ (4.1 and after) when the
                // notification is expanded.
                .setStyle(inboxStyle)

                // Title for API <16 (4.0 and below) devices and API 16+ (4.1 and after) when the
                // notification is collapsed.
                .setContentTitle(inboxStyleEmailAppData.contentTitle)

                // Content for API <24 (7.0 and below) devices and API 16+ (4.1 and after) when the
                // notification is collapsed.
                .setContentText(inboxStyleEmailAppData.contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                // Sets large number at the right-hand side of the notification for API <24 devices.
                .setSubText(Integer.toString(inboxStyleEmailAppData.numberOfNewEmails))

                .setCategory(Notification.CATEGORY_EMAIL)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(inboxStyleEmailAppData.priority)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(inboxStyleEmailAppData.channelLockscreenVisibility)

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (name in inboxStyleEmailAppData.participants) {
            notificationCompatBuilder.addPerson(name)
        }

        val notification = notificationCompatBuilder.build()

        mNotificationManagerCompat!!.notify(NOTIFICATION_ID, notification)
    }

    /*
     * Generates a MESSAGING_STYLE Notification that supports both phone/tablet and wear. For
     * devices on API level 24 (7.0 - Nougat) and after, displays MESSAGING_STYLE. Otherwise,
     * displays a basic BIG_TEXT_STYLE.
     */
    private fun generateMessagingStyleNotification() {
        Log.d(TAG, "generateMessagingStyleNotification()")

        // 0. Get your data (everything unique per Notification)
        val messagingStyleCommsAppData = MockDatabase.getMessagingStyleData(applicationContext)

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId = NotificationUtil.createNotificationChannel(this, messagingStyleCommsAppData)

        // 2. Build the NotificationCompat.Style (MESSAGING_STYLE).
        val contentTitle = messagingStyleCommsAppData.contentTitle

        val messagingStyle = MessagingStyle(messagingStyleCommsAppData.me)
                /*
                         * <p>This API's behavior was changed in SDK version
                         * {@link Build.VERSION_CODES#P}. If your application's target version is
                         * less than {@link Build.VERSION_CODES#P}, setting a conversation title to
                         * a non-null value will make {@link #isGroupConversation()} return
                         * {@code true} and passing {@code null} will make it return {@code false}.
                         * This behavior can be overridden by calling
                         * {@link #setGroupConversation(boolean)} regardless of SDK version.
                         * In {@code P} and above, this method does not affect group conversation
                         * settings.
                         *
                         * In our case, we use the same title.
                         */
                .setConversationTitle("Group title")
                .setGroupConversation(true)


        // Adds all Messages.
        // Note: Messages include the text, timestamp, and sender.
        for (message in messagingStyleCommsAppData.messages) {
            messagingStyle.addMessage(message)
        }


        messagingStyle.isGroupConversation = messagingStyleCommsAppData.isGroupConversation

        // 3. Set up main Intent for notification.
        val notifyIntent = Intent(this, MessagingMainActivity::class.java)

        val stackBuilder = TaskStackBuilder.create(this)
        // Adds the back stack
        stackBuilder.addParentStack(MessagingMainActivity::class.java!!)
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(notifyIntent)
        // Gets a PendingIntent containing the entire back stack
        val mainPendingIntent = PendingIntent.getActivity(
                this,
                0,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )


        // 4. Set up RemoteInput, so users can input (keyboard and voice) from notification.

        // Note: For API <24 (M and below) we need to use an Activity, so the lock-screen present
        // the auth challenge. For API 24+ (N and above), we use a Service (could be a
        // BroadcastReceiver), so the user can input from Notification or lock-screen (they have
        // choice to allow) without leaving the notification.

        // Create the RemoteInput specifying this key.
        val replyLabel = getString(R.string.reply_label)
        val remoteInput = RemoteInput.Builder(MessagingIntentService.EXTRA_REPLY)
                .setLabel(replyLabel)
                // Use machine learning to create responses based on previous messages.
                //.setChoices(messagingStyleCommsAppData.getReplyChoicesBasedOnLastMessage())
                .build()

        // Pending intent =
        //      API <24 (M and below): activity so the lock-screen presents the auth challenge.
        //      API 24+ (N and above): this should be a Service or BroadcastReceiver.
        val replyActionPendingIntent: PendingIntent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(this, MessagingIntentService::class.java)
            intent.action = MessagingIntentService.ACTION_REPLY
            replyActionPendingIntent = PendingIntent.getService(this, 0, intent, 0)

        } else {
            replyActionPendingIntent = mainPendingIntent
        }

        val replyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_reply_white_18dp,
                replyLabel,
                replyActionPendingIntent)
                .addRemoteInput(remoteInput)
                // Informs system we aren't bringing up our own custom UI for a reply
                // action.
                .setShowsUserInterface(false)
                // Allows system to generate replies by context of conversation.
                .setAllowGeneratedReplies(true)
                .setSemanticAction(Action.SEMANTIC_ACTION_REPLY)
                .build()


        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating current notification), we
        // create a new Builder. Later, we update this same notification, so we need to save this
        // Builder globally (as outlined earlier).

        val notificationCompatBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder)

        notificationCompatBuilder
                // MESSAGING_STYLE sets title and content for API 16 and above devices.
                .setStyle(messagingStyle)
                // Title for API < 16 devices.
                .setContentTitle(contentTitle)
                // Content for API < 16 devices.
                .setContentText(messagingStyleCommsAppData.contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                // Number of new notifications for API <24 (M and below) devices.
                .setSubText(Integer.toString(messagingStyleCommsAppData.numberOfNewMessages))

                .addAction(replyAction)
                .setCategory(Notification.CATEGORY_MESSAGE)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(messagingStyleCommsAppData.priority)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(messagingStyleCommsAppData.channelLockscreenVisibility)

        // If the phone is in "Do not disturb" mode, the user may still be notified if the
        // sender(s) are in a group allowed through "Do not disturb" by the user.
        for (name in messagingStyleCommsAppData.participants) {
            notificationCompatBuilder.addPerson(name.uri)
        }

        val notification = notificationCompatBuilder.build()
        mNotificationManagerCompat!!.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Helper method for the SnackBar action, i.e., if the user has this application's notifications
     * disabled, this opens up the dialog to turn them back on after the user requests a
     * Notification launch.
     *
     * IMPORTANT NOTE: You should not do this action unless the user takes an action to see your
     * Notifications like this sample demonstrates. Spamming users to re-enable your notifications
     * is a bad idea.
     */
    private fun openNotificationSettingsForApp() {
        // Links to this app's notification settings.
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("app_package", packageName)
        intent.putExtra("app_uid", applicationInfo.uid)
        startActivity(intent)
    }

    companion object {

        val TAG = "MainActivity"

        val NOTIFICATION_ID = Random().nextInt(1000)

        // Used for Notification Style array and switch statement for Spinner selection.
        private val BIG_TEXT_STYLE = "BIG_TEXT_STYLE"
        private val BIG_PICTURE_STYLE = "BIG_PICTURE_STYLE"
        private val INBOX_STYLE = "INBOX_STYLE"
        private val MESSAGING_STYLE = "MESSAGING_STYLE"
        private val MEDIA_STYLE = "MEDIA_STYLE"
        private val PROGRESS_STYLE = "PROGRESS_STYLE"
        private val BUNDLED_NOTIFICATION = "BUNDLED_NOTIFICATION"
        private val SMART_NOTIFICATION = "SMART_NOTIFICATION"
        private val NOTIFICATION_BUBBLE = "NOTIFICATION_BUBBLE"

        // Collection of notification styles to back ArrayAdapter for Spinner.
        private val NOTIFICATION_STYLES = arrayOf(
                BIG_TEXT_STYLE,
                BIG_PICTURE_STYLE,
                INBOX_STYLE,
                MESSAGING_STYLE,
                MEDIA_STYLE,
                PROGRESS_STYLE,
                BUNDLED_NOTIFICATION,
                SMART_NOTIFICATION,
                NOTIFICATION_BUBBLE
        )

        private val NOTIFICATION_STYLES_DESCRIPTION = arrayOf(
                "Demos reminder type app using BIG_TEXT_STYLE",
                "Demos social type app using BIG_PICTURE_STYLE + inline notification response",
                "Demos email type app using INBOX_STYLE",
                "Demos messaging app using MESSAGING_STYLE + inline notification responses",
                "Demos messaging app using MEDIA_STYLE",
                "Demos messaging app using PROGRESS_STYLE",
                "Demos messaging app using BUNDLED_NOTIFICATION",
                "Demos messaging app using SMART_NOTIFICATION",
                "Demos messaging app using NOTIFICATION_BUBBLE"
        )
    }
}

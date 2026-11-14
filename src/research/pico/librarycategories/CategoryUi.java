package research.pico.librarycategories;

import android.app.*;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.util.*;
import de.robv.android.xposed.*;

final class CategoryUi {
    private static int managerWidth = 0;
 
    private static void triggerHaptic(View view, MotionEvent event, boolean isHover) {
        if (view == null) return;
        Context context = view.getContext();
        if (context == null) return;
        ClassLoader cl = context.getClassLoader();
        if (cl == null) cl = CategoryUi.class.getClassLoader();

        int effectType = isHover ? 0 : 1;
        float freq = isHover ? 177f : 204f;
        float amp = isHover ? 33f : 80f;
        float dur = isHover ? 5f : 10f;

        int devId = (event != null) ? event.getDeviceId() : 0;
        int osuiDeviceId;
        int nativeDeviceId;

        if (devId == 100001 || devId == 1) {
            osuiDeviceId = 100001;
            nativeDeviceId = 1;
        } else if (devId == 100002 || devId == 2) {
            osuiDeviceId = 100002;
            nativeDeviceId = 2;
        } else if (devId > 0) {
            osuiDeviceId = devId;
            nativeDeviceId = (devId == 100001) ? 1 : 2;
        } else {
            osuiDeviceId = 100002;
            nativeDeviceId = 2;
        }

        boolean success = false;
        try {
            Class<?> effectClass = cl.loadClass("c.a.h.n.b");
            try {
                java.lang.reflect.Method xMethod = effectClass.getMethod("x", Context.class);
                xMethod.invoke(null, context);
            } catch (Throwable ignored) {}
            java.lang.reflect.Method pMethod = effectClass.getMethod("p", Context.class, int.class, int.class);
            pMethod.invoke(null, context, effectType, osuiDeviceId);
            success = true;
        } catch (Throwable ignored) {}

        if (!success) {
            try {
                Class<?> helperClass = cl.loadClass("com.bytedance.osui.utils.OSUIHelper");
                java.lang.reflect.Method aMethod = helperClass.getMethod("a", int.class, float.class, float.class, float.class);
                aMethod.invoke(null, nativeDeviceId, freq, amp, dur);
                success = true;
            } catch (Throwable ignored) {}
        }

        if (!success) {
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            } catch (Throwable ignored) {}
        }
    }

    private static void triggerHaptic(View view, boolean isHover) {
        triggerHaptic(view, null, isHover);
    }

    private static void applyHaptic(View view) {
        if (view == null) return;
        view.setOnHoverListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_HOVER_ENTER) {
                v.setHovered(true);
                triggerHaptic(v, event, true);
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                v.setHovered(false);
            }
            return false;
        });
    }

    private static void setClickWithHaptic(View view, View.OnClickListener listener) {
        if (view == null) return;
        final MotionEvent[] lastEvent = new MotionEvent[1];
        view.setOnHoverListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_HOVER_ENTER) {
                v.setHovered(true);
                triggerHaptic(v, event, true);
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                v.setHovered(false);
            }
            lastEvent[0] = event;
            return false;
        });
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastEvent[0] = event;
            }
            return false;
        });
        view.setOnClickListener(v -> {
            triggerHaptic(v, lastEvent[0], false);
            if (listener != null) listener.onClick(v);
        });
    }

    private static int dialogWidth(Context c) {
        return managerWidth > 0 ? managerWidth : (int) (540 * c.getResources().getDisplayMetrics().density);
    }
    private static AlertDialog showCustomDialog(Activity a, View dialogBox, int fixedWidth) {
        if (managerWidth <= 0 && dialogBox.getWidth() > 0) {
            managerWidth = dialogBox.getWidth();
        }
        int w = managerWidth > 0 ? managerWidth : fixedWidth;

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setView(dialogBox)
                .create();

        if (dialog.getWindow() != null) {
            float density = a.getResources().getDisplayMetrics().density;
            int dp32 = (int) (32 * density);
            Drawable bg = roundRect(0xFF1F1F21, 20f, a);
            InsetDrawable inset = new InsetDrawable(bg, 0, dp32, 0, dp32);
            dialog.getWindow().setBackgroundDrawable(inset);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
        dialog.show();
        return dialog;
    }
    private static Drawable createScrollThumb(Context c) {
        float density = c.getResources().getDisplayMetrics().density;
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.RECTANGLE);
        thumb.setColor(0xFF323232);
        thumb.setCornerRadius(3 * density);
        thumb.setSize((int) (6 * density), (int) (32 * density));
        return thumb;
    }
    private static GradientDrawable roundRect(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp * c.getResources().getDisplayMetrics().density);
        return d;
    }

    private static StateListDrawable hoverableRect(int normalColor, int hoverColor, float radiusDp, Context c) {
        StateListDrawable sld = new StateListDrawable();
        float r = radiusDp * c.getResources().getDisplayMetrics().density;
        GradientDrawable hover = new GradientDrawable();
        hover.setColor(hoverColor);
        hover.setCornerRadius(r);
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(r);
        sld.addState(new int[]{android.R.attr.state_pressed}, hover);
        sld.addState(new int[]{android.R.attr.state_focused}, hover);
        sld.addState(new int[]{android.R.attr.state_hovered}, hover);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    private static StateListDrawable buttonDrawable(int normalColor, int pressedColor, float radiusDp, Context c) {
        StateListDrawable sld = new StateListDrawable();
        float r = radiusDp * c.getResources().getDisplayMetrics().density;
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(pressedColor);
        pressed.setCornerRadius(r);
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(r);
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{android.R.attr.state_focused}, pressed);
        sld.addState(new int[]{android.R.attr.state_hovered}, pressed);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    private static View createCircleIconButton(Context c, String symbol, int bgColor, int sizeDp) {
        float density = c.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);

        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.OVAL);
        normal.setColor(bgColor);

        GradientDrawable hover = new GradientDrawable();
        hover.setShape(GradientDrawable.OVAL);
        int hoverColor = (bgColor == 0xFFfe645c) ? 0xFFff857e : ((bgColor == 0xFF8cd200) ? 0xFFa2e61a : 0xFF58585A);
        hover.setColor(hoverColor);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, hover);
        sld.addState(new int[]{android.R.attr.state_focused}, hover);
        sld.addState(new int[]{android.R.attr.state_hovered}, hover);
        sld.addState(new int[]{}, normal);

        TextView tv = new TextView(c);
        tv.setText(symbol);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * 0.68f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(sld);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static void tintSwitch(Switch sw, int checkedColor, int uncheckedColor) {
        float density = sw.getResources().getDisplayMetrics().density;
        
        int trackWidth = (int) (34 * density);
        int trackHeight = (int) (18 * density);
        
        GradientDrawable checkedTrack = new GradientDrawable();
        checkedTrack.setShape(GradientDrawable.RECTANGLE);
        checkedTrack.setColor(checkedColor);
        checkedTrack.setSize(trackWidth, trackHeight);
        checkedTrack.setCornerRadius(trackHeight / 2f);

        GradientDrawable uncheckedTrack = new GradientDrawable();
        uncheckedTrack.setShape(GradientDrawable.RECTANGLE);
        uncheckedTrack.setColor(0xFF3A3A3C);
        uncheckedTrack.setSize(trackWidth, trackHeight);
        uncheckedTrack.setCornerRadius(trackHeight / 2f);

        StateListDrawable trackSld = new StateListDrawable();
        trackSld.addState(new int[]{android.R.attr.state_checked}, checkedTrack);
        trackSld.addState(new int[]{}, uncheckedTrack);

        int thumbSize = (int) (16 * density);
        GradientDrawable thumbDrawable = new GradientDrawable();
        thumbDrawable.setShape(GradientDrawable.OVAL);
        thumbDrawable.setColor(0xFFFFFFFF);
        thumbDrawable.setSize(thumbSize, thumbSize);

        sw.setTrackDrawable(trackSld);
        sw.setThumbDrawable(thumbDrawable);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            sw.setShowText(false);
        }
    }

    private static Drawable createCheckmarkDrawable(Context c, int color) {
        float density = c.getResources().getDisplayMetrics().density;
        int size = (int) (22 * density);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(2.2f * density);
        paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);

        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(size * 0.30f, size * 0.52f);
        path.lineTo(size * 0.45f, size * 0.67f);
        path.lineTo(size * 0.72f, size * 0.35f);
        canvas.drawPath(path, paint);

        return new android.graphics.drawable.BitmapDrawable(c.getResources(), bitmap);
    }

    private static void tintCheckBox(CheckBox cb, int color) {
        Context c = cb.getContext();
        float density = c.getResources().getDisplayMetrics().density;
        int size = (int) (22 * density);
        float radius = 5 * density;

        GradientDrawable uncheckedDraw = new GradientDrawable();
        uncheckedDraw.setShape(GradientDrawable.RECTANGLE);
        uncheckedDraw.setColor(0x00000000);
        uncheckedDraw.setCornerRadius(radius);
        uncheckedDraw.setStroke((int) Math.max(1, 1.5f * density), 0xFFA6A6A6);
        uncheckedDraw.setSize(size, size);

        GradientDrawable checkedBg = new GradientDrawable();
        checkedBg.setShape(GradientDrawable.RECTANGLE);
        checkedBg.setColor(color);
        checkedBg.setCornerRadius(radius);
        checkedBg.setSize(size, size);

        Drawable checkmark = createCheckmarkDrawable(c, 0xFFFFFFFF);

        LayerDrawable checkedDraw = new LayerDrawable(new Drawable[] { checkedBg, checkmark });

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[] { android.R.attr.state_checked }, checkedDraw);
        sld.addState(new int[] { -android.R.attr.state_checked }, uncheckedDraw);
        sld.addState(new int[] {}, uncheckedDraw);

        cb.setButtonDrawable(sld);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            cb.setButtonTintList(null);
        }
    }

    private static boolean save(Activity a, Categories data, Runnable changed) {
        try {
            data.save();
        } catch (Throwable failure) {
            XposedBridge.log("PICO Categories: save failed " + failure);
            Toast.makeText(a, "Could not save categories", Toast.LENGTH_LONG).show();
            return false;
        }
        try {
            changed.run();
        } catch (Throwable failure) {
            XposedBridge.log("PICO Categories: refresh failed " + failure);
            Toast.makeText(a, "Saved. Reopen Library to refresh.", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private interface NameResult {
        void accept(String name);
    }

    private static void RenameDialog(Activity a, String title, String initial, NameResult result) {
        float density = a.getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp12 = (int) (12 * density);
        int dp42 = (int) (42 * density);

        LinearLayout container = new LinearLayout(a);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16, dp16, dp16);
        container.setMinimumWidth((int) (400 * density));

        TextView titleTv = new TextView(a);
        titleTv.setText(title);
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(18);
        titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, 0, 0, dp16);
        container.addView(titleTv);

        EditText edit = new EditText(a);
        edit.setSingleLine(true);
        edit.setText(initial);
        edit.selectAll();
        edit.setTextColor(0xFFFFFFFF);
        edit.setHintTextColor(0xFFA6A6A6);
        edit.setTextSize(16);
        edit.setBackground(roundRect(0xFF2C2C2E, 10f, a));
        edit.setPadding(dp16, dp12, dp16, dp12);
        applyHaptic(edit);
        edit.setOnClickListener(v -> triggerHaptic(v, false));
        edit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) triggerHaptic(v, false);
        });
        edit.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                triggerHaptic(edit, true);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        container.addView(edit);

        LinearLayout footer = new LinearLayout(a);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2);
        footerLp.topMargin = dp16;
        footer.setLayoutParams(footerLp);

        AlertDialog d = new AlertDialog.Builder(a)
                .setView(container)
                .create();

        Button neg = new Button(a);
        neg.setText("Cancel");
        neg.setTextSize(15);
        neg.setAllCaps(false);
        neg.setGravity(Gravity.CENTER);
        neg.setTextColor(0xFFDCDCDC);
        neg.setBackground(buttonDrawable(0xFF3A3A3C, 0xFF48484A, 10f, a));
        setClickWithHaptic(neg, button -> d.dismiss());

        Button pos = new Button(a);
        pos.setText("Save");
        pos.setTextSize(15);
        pos.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pos.setAllCaps(false);
        pos.setGravity(Gravity.CENTER);
        pos.setTextColor(0xFFFFFFFF);
        pos.setBackground(buttonDrawable(0x773e8bff, 0x553e8bff, 10f, a));
        setClickWithHaptic(pos, button -> {
            String value = edit.getText().toString().trim();
            if (value.isEmpty() || value.length() > 40) {
                edit.setError("Use 1–40 characters");
                return;
            }
            d.dismiss();
            result.accept(value);
        });

        footer.addView(neg, new LinearLayout.LayoutParams(0, dp42, 1f));
        Space space = new Space(a);
        footer.addView(space, new LinearLayout.LayoutParams(dp12, -1));
        footer.addView(pos, new LinearLayout.LayoutParams(0, dp42, 1f));

        container.addView(footer);

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(roundRect(0xFF1F1F21, 20f, a));
        }
        d.show();
    }

    private static void ConfirmDialog(Activity a, String title, String message, Runnable onConfirm) {
        float density = a.getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp12 = (int) (12 * density);
        int dp42 = (int) (42 * density);

        LinearLayout container = new LinearLayout(a);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16, dp16, dp16);
        container.setMinimumWidth((int) (400 * density));

        TextView titleTv = new TextView(a);
        titleTv.setText(title);
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(18);
        titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, 0, 0, (int) (8 * density));
        container.addView(titleTv);

        TextView msgTv = new TextView(a);
        msgTv.setText(message);
        msgTv.setTextColor(0xFFA6A6A6);
        msgTv.setTextSize(14);
        msgTv.setPadding(0, 0, 0, dp16);
        container.addView(msgTv);

        LinearLayout footer = new LinearLayout(a);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2);
        footerLp.topMargin = dp16;
        footer.setLayoutParams(footerLp);

        AlertDialog d = new AlertDialog.Builder(a)
                .setView(container)
                .create();

        Button neg = new Button(a);
        neg.setText("Cancel");
        neg.setTextSize(15);
        neg.setAllCaps(false);
        neg.setGravity(Gravity.CENTER);
        neg.setTextColor(0xFFDCDCDC);
        neg.setBackground(buttonDrawable(0xFF3A3A3C, 0xFF48484A, 10f, a));
        setClickWithHaptic(neg, button -> d.dismiss());

        Button pos = new Button(a);
        pos.setText("Delete");
        pos.setTextSize(15);
        pos.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pos.setAllCaps(false);
        pos.setGravity(Gravity.CENTER);
        pos.setTextColor(0xFFDCDCDC);
        pos.setBackground(buttonDrawable(0xFFfe645c, 0x55fe645c, 10f, a));
        setClickWithHaptic(pos, button -> {
            d.dismiss();
            onConfirm.run();
        });

        footer.addView(neg, new LinearLayout.LayoutParams(0, dp42, 1f));
        Space space = new Space(a);
        footer.addView(space, new LinearLayout.LayoutParams(dp12, -1));
        footer.addView(pos, new LinearLayout.LayoutParams(0, dp42, 1f));

        container.addView(footer);

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(roundRect(0xFF1F1F21, 20f, a));
        }
        d.show();
    }

    static void ForceStopConfirmDialog(Activity a, String appPkg, Runnable onConfirm) {
        float density = a.getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp12 = (int) (12 * density);
        int dp42 = (int) (42 * density);

        LinearLayout container = new LinearLayout(a);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16, dp16, dp16);
        container.setMinimumWidth((int) (400 * density));

        TextView titleTv = new TextView(a);
        titleTv.setText("Force Stop?");
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(18);
        titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, 0, 0, (int) (8 * density));
        container.addView(titleTv);

        TextView msgTv = new TextView(a);
        msgTv.setText("Force stopping an app might lead to errors.");
        msgTv.setTextColor(0xFFA6A6A6);
        msgTv.setTextSize(14);
        msgTv.setPadding(0, 0, 0, dp16);
        container.addView(msgTv);

        LinearLayout footer = new LinearLayout(a);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2);
        footerLp.topMargin = dp16;
        footer.setLayoutParams(footerLp);

        AlertDialog d = new AlertDialog.Builder(a)
                .setView(container)
                .create();

        Button neg = new Button(a);
        neg.setText("Cancel");
        neg.setTextSize(15);
        neg.setAllCaps(false);
        neg.setGravity(Gravity.CENTER);
        neg.setTextColor(0xFFDCDCDC);
        neg.setBackground(buttonDrawable(0xFF3A3A3C, 0xFF48484A, 10f, a));
        setClickWithHaptic(neg, button -> d.dismiss());

        Button pos = new Button(a);
        pos.setText("Confirm");
        pos.setTextSize(15);
        pos.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pos.setAllCaps(false);
        pos.setGravity(Gravity.CENTER);
        pos.setTextColor(0xFFFFFFFF);
        pos.setBackground(buttonDrawable(0x773e8bff, 0x553e8bff, 10f, a));
        setClickWithHaptic(pos, button -> {
            d.dismiss();
            onConfirm.run();
        });

        footer.addView(neg, new LinearLayout.LayoutParams(0, dp42, 1f));
        Space space = new Space(a);
        footer.addView(space, new LinearLayout.LayoutParams(dp12, -1));
        footer.addView(pos, new LinearLayout.LayoutParams(0, dp42, 1f));

        container.addView(footer);

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(roundRect(0xFF1F1F21, 20f, a));
        }
        d.show();
    }

    static void CategoriesManager(Activity a, Categories data, Runnable changed) {
        if (a.isFinishing()) return;
        int fixedWidth = dialogWidth(a);
        float density = a.getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp12 = (int) (12 * density);
        int dp10 = (int) (10 * density);
        int dp8 = (int) (8 * density);

        LinearLayout dialogBox = new LinearLayout(a);
        dialogBox.setOrientation(LinearLayout.VERTICAL);
        dialogBox.setPadding(dp16, dp16, dp8, dp16);
        dialogBox.setLayoutParams(new LinearLayout.LayoutParams(fixedWidth, -2));
        dialogBox.setMinimumWidth(fixedWidth);

        // Header Title
        TextView titleTv = new TextView(a);
        titleTv.setText("Library Categories");
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(18);
        titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, 0, 0, dp16);
        dialogBox.addView(titleTv);

        // Scrollable content area
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout helpCard = new LinearLayout(a);
        helpCard.setBackground(roundRect(0xFF2C2C2E, 12f, a));
        helpCard.setPadding(dp12, dp8, dp12, dp8);
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(-1, -2);
        helpLp.bottomMargin = dp8;
        helpCard.setLayoutParams(helpLp);

        TextView help = new TextView(a);
        help.setText("Hold the ☰ handle and drag to reorder. Tap buttons to rename, hide, or pick apps.");
        help.setTextColor(0xFFA6A6A6);
        help.setTextSize(13);
        helpCard.addView(help);
        box.addView(helpCard);

        ScrollView scroll = new ScrollView(a) {
            { setPadding(0, 0, dp8, 0); setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET); }
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int maxHeight = (int) (480 * density);
                int hMode = MeasureSpec.getMode(heightMeasureSpec);
                int hSize = MeasureSpec.getSize(heightMeasureSpec);
                if (hMode == MeasureSpec.UNSPECIFIED || hSize > maxHeight) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollbarThumbDrawable(createScrollThumb(a));
        box.setLayoutParams(new ScrollView.LayoutParams(-1, -2));

        DragCategoryList list = new DragCategoryList(a, scroll, (from, to) -> {
            Categories.Category c = data.items.get(from);
            data.moveTo(c, to);
            if (save(a, data, changed)) return true;
            data.moveTo(c, from);
            return false;
        });
        box.addView(list);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        for (Categories.Category c : new ArrayList<>(data.items)) {
            LinearLayout card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(roundRect(0xFF2C2C2E, 14f, a));
            card.setPadding(dp16, dp8, dp16, dp8);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.bottomMargin = dp6_or_8(density, 6);
            card.setLayoutParams(cardLp);

            LinearLayout topRow = new LinearLayout(a);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            // Icon on left of category name
            ImageView categoryIcon = new ImageView(a);
            Drawable iconDrawable = null;
            if (c.custom()) {
                iconDrawable = a.getDrawable(android.R.drawable.ic_menu_agenda);
            } else {
                int stockIconRes = 0;
                if ("all".equals(c.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_all", "drawable", "com.pvr.appmanager");
                else if ("app".equals(c.id) || "apps".equals(c.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_application", "drawable", "com.pvr.appmanager");
                else if ("game".equals(c.id) || "entertainment".equals(c.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_game", "drawable", "com.pvr.appmanager");
                else if ("update".equals(c.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_update", "drawable", "com.pvr.appmanager");

                if (stockIconRes != 0) {
                    try { iconDrawable = a.getDrawable(stockIconRes); } catch (Throwable ignored) {}
                }
            }
            if (iconDrawable == null) {
                iconDrawable = a.getDrawable(android.R.drawable.ic_menu_agenda);
            }

            categoryIcon.setImageDrawable(iconDrawable);
            categoryIcon.setColorFilter(c.hidden ? 0xFFA6A6A6 : 0xFFFFFFFF);
            int iconSize = (int) (20 * density);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.rightMargin = dp8;
            topRow.addView(categoryIcon, iconLp);

            // Category Name Title
            TextView title = new TextView(a);
            title.setText(c.name);
            title.setTextColor(c.hidden ? 0xFFA6A6A6 : 0xFFFFFFFF);
            title.setTextSize(16);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            topRow.addView(title, new LinearLayout.LayoutParams(-2, -2));

            // Red minus button right next to category name (for custom categories that can be deleted)
            if (c.custom() && !c.id.equals(CategoryModel.HIDDEN_ID)) {
                View minusBtn = createCircleIconButton(a, "−", 0xFFfe645c, 14);
                LinearLayout.LayoutParams minusLp = (LinearLayout.LayoutParams) minusBtn.getLayoutParams();
                minusLp.leftMargin = dp8;
                minusLp.rightMargin = dp8;
                topRow.addView(minusBtn, minusLp);

                setClickWithHaptic(minusBtn, v -> {
                    ConfirmDialog(a, "Delete " + c.name + "?", "Apps stay installed. Only this category is removed.", () -> {
                        data.items.remove(c);
                        save(a, data, changed);
                        if (dialogRef[0] != null) dialogRef[0].dismiss();
                        CategoriesManager(a, data, changed);
                    });
                });
            }

            // Spacer to push switch and handle to the far right
            Space spacer = new Space(a);
            topRow.addView(spacer, new LinearLayout.LayoutParams(0, -2, 1f));

            // Toggle Switch for Show/Hide to the left of drag handle
            Switch hideSwitch = new Switch(a);
            hideSwitch.setChecked(!c.hidden); // Checked = shown, Unchecked = hidden
            hideSwitch.setPadding(dp8, 0, dp8, 0);
            tintSwitch(hideSwitch, 0x773e8bff, 0xFF48484A);
            applyHaptic(hideSwitch);
            hideSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                triggerHaptic(buttonView, false);
                c.hidden = !isChecked; // isChecked=true -> c.hidden=false, isChecked=false -> c.hidden=true
                save(a, data, changed);
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                CategoriesManager(a, data, changed);
            });
            LinearLayout.LayoutParams switchLp = new LinearLayout.LayoutParams(-2, -2);
            switchLp.rightMargin = dp8;
            topRow.addView(hideSwitch, switchLp);

            // Drag Handle ☰ on far right
            TextView handle = new TextView(a);
            handle.setText("☰");
            handle.setTextSize(22);
            handle.setTextColor(0xFF6a6a6a);
            handle.setGravity(Gravity.CENTER);
            handle.setPadding(dp10, 0, dp10, 0);
            handle.setClickable(true);
            handle.setBackground(buttonDrawable(0xFF292c2b, 0xFF373a39, 10f, a));
            applyHaptic(handle);
            topRow.addView(handle);

            card.addView(topRow);

            LinearLayout btnRow = new LinearLayout(a);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setWeightSum(2f);
            LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(-1, -2);
            btnRowLp.topMargin = dp6_or_8(density, 6);
            btnRow.setLayoutParams(btnRowLp);

            list.addRow(card, handle);

            addStyledBtn(btnRow, "Rename", 0xFF3A3A3C, 0xFF48484A, 0xFFFFFFFF, () -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                RenameDialog(a, "Rename Category", c.name, value -> {
                    c.name = value;
                    save(a, data, changed);
                    CategoriesManager(a, data, changed);
                });
            });

            boolean showAppsBtn = c.custom()
                || "app".equalsIgnoreCase(c.id) || "apps".equalsIgnoreCase(c.id)
                || "game".equalsIgnoreCase(c.id) || "entertainment".equalsIgnoreCase(c.id)
                || "unknown".equalsIgnoreCase(c.id);

            if (showAppsBtn) {
                addStyledBtn(btnRow, "Apps…", 0xFF3A3A3C, 0xFF48484A, 0xFFFFFFFF, () -> {
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    AppsPicker(a, data, c, changed);
                });
            }

            card.addView(btnRow);
        }

        // Bottom standalone green plus (+) button row (no background card, button only)
        LinearLayout newRow = new LinearLayout(a);
        newRow.setOrientation(LinearLayout.HORIZONTAL);
        newRow.setGravity(Gravity.CENTER);
        newRow.setPadding(0, dp8, 0, dp8);

        LinearLayout.LayoutParams newRowLp = new LinearLayout.LayoutParams(-1, -2);
        newRowLp.topMargin = dp6_or_8(density, 6);
        newRowLp.bottomMargin = dp6_or_8(density, 6);
        newRow.setLayoutParams(newRowLp);

        View plusBtn = createCircleIconButton(a, "+", 0xFF8cd200, 28);
        plusBtn.setFocusable(true);
        plusBtn.setClickable(true);
        setClickWithHaptic(plusBtn, v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            RenameDialog(a, "New category", "", value -> {
                data.create(value);
                save(a, data, changed);
                CategoriesManager(a, data, changed);
            });
        });

        newRow.addView(plusBtn);
        box.addView(newRow);

        scroll.addView(box);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialogBox.addView(scroll, scrollLp);

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setView(dialogBox)
                .create();
        dialogRef[0] = dialog;

        dialog.setOnShowListener(d -> {
            if (dialogBox.getWidth() > 0) {
                managerWidth = dialogBox.getWidth();
            }
        });
        dialogBox.post(() -> {
            if (dialogBox.getWidth() > 0) {
                managerWidth = dialogBox.getWidth();
            }
        });

        if (dialog.getWindow() != null) {
            int dp32 = (int) (32 * density);
            Drawable bg = roundRect(0xFF1F1F21, 20f, a);
            InsetDrawable inset = new InsetDrawable(bg, 0, dp32, 0, dp32);
            dialog.getWindow().setBackgroundDrawable(inset);
            dialog.getWindow().setLayout(fixedWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        dialog.show();
    }

    private static int dp6_or_8(float density, int val) {
        return Math.round(val * density);
    }

    private static int dp10_or_12(float density, int val) {
        return Math.round(val * density);
    }

    private static void addStyledBtn(LinearLayout row, String text, int bgColor, int pressColor, int textColor, Runnable action) {
        Context c = row.getContext();
        float density = c.getResources().getDisplayMetrics().density;
        int dp4 = (int) (4 * density);

        Button b = new Button(c);
        b.setText(text);
        b.setTextColor(textColor);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackground(buttonDrawable(bgColor, pressColor, 8f, c));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (34 * density), 1);
        lp.rightMargin = dp4;
        lp.leftMargin = dp4;

        row.addView(b, lp);
        setClickWithHaptic(b, v -> action.run());
    }

    static void CategoryAssigner(Activity a, Categories data, String pkg, String label, Runnable changed) {
        List<Categories.Category> list = new ArrayList<>();
        for (Categories.Category c : data.items) {
            if (c.id.equals(CategoryModel.HIDDEN_ID)) continue;
            if ("all".equalsIgnoreCase(c.id)) continue;
            if ("update".equalsIgnoreCase(c.id)) continue;
            if ("not_installed".equalsIgnoreCase(c.id)) continue;
            list.add(c);
        }
        int fixedWidth = dialogWidth(a);
        float density = a.getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp12 = (int) (12 * density);
        int dp8 = (int) (8 * density);

        LinearLayout dialogBox = new LinearLayout(a);
        dialogBox.setOrientation(LinearLayout.VERTICAL);
        dialogBox.setPadding(dp16, dp16, dp8, dp16);
        dialogBox.setLayoutParams(new LinearLayout.LayoutParams(fixedWidth, -2));
        dialogBox.setMinimumWidth(fixedWidth);

        TextView titleTv = new TextView(a);
        titleTv.setText(label + " - Categories");
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(18);
        titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleTv.setPadding(0, 0, 0, dp16);
        dialogBox.addView(titleTv);

        boolean[] selected = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Categories.Category cat = list.get(i);
            boolean isMember = cat.members.contains(pkg);
            if (!isMember && !cat.exclusions.contains(pkg) && !cat.custom()) {
                try {
                    Object filter = XposedHelpers.callMethod(a, "getMFilter");
                    Object targetFilter = XposedHelpers.callMethod(filter, "getTargetFilter", cat.id);
                    if (targetFilter == null && ("app".equalsIgnoreCase(cat.id) || "apps".equalsIgnoreCase(cat.id))) {
                        targetFilter = XposedHelpers.callMethod(filter, "getTargetFilter", "app");
                    }
                    if (targetFilter == null && ("game".equalsIgnoreCase(cat.id) || "entertainment".equalsIgnoreCase(cat.id))) {
                        targetFilter = XposedHelpers.callMethod(filter, "getTargetFilter", "game");
                    }
                    if (targetFilter != null) {
                        List<?> masterApps = (List<?>) XposedHelpers.callMethod(a, "getMAllApp");
                        if (masterApps != null) {
                            for (Object app : masterApps) {
                                if (pkg.equals(HookEntry.pkg(app))) {
                                    isMember = (Boolean) XposedHelpers.callMethod(targetFilter, "c", app);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            selected[i] = isMember;
        }

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutParams(new ScrollView.LayoutParams(-1, -2));

        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            Categories.Category cat = list.get(idx);

            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(hoverableRect(0xFF2C2C2E, 0xFF3A3A3C, 12f, a));
            row.setPadding(dp16, dp12, dp16, dp12);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.bottomMargin = dp6_or_8(density, 6);
            row.setLayoutParams(rowLp);

            CheckBox checkBox = new CheckBox(a);
            checkBox.setChecked(selected[idx]);
            checkBox.setClickable(false);
            tintCheckBox(checkBox, 0x773e8bff);
            applyHaptic(checkBox);
            row.addView(checkBox);

            ImageView categoryIcon = new ImageView(a);
            Drawable iconDrawable = null;
            if (cat.custom()) {
                iconDrawable = a.getDrawable(android.R.drawable.ic_menu_agenda);
            } else {
                int stockIconRes = 0;
                if ("all".equals(cat.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_all", "drawable", "com.pvr.appmanager");
                else if ("app".equals(cat.id) || "apps".equals(cat.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_application", "drawable", "com.pvr.appmanager");
                else if ("game".equals(cat.id) || "entertainment".equals(cat.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_game", "drawable", "com.pvr.appmanager");
                else if ("update".equals(cat.id)) stockIconRes = a.getResources().getIdentifier("ic_icon_navi_update", "drawable", "com.pvr.appmanager");

                if (stockIconRes != 0) {
                    try { iconDrawable = a.getDrawable(stockIconRes); } catch (Throwable ignored) {}
                }
            }
            if (iconDrawable == null) {
                iconDrawable = a.getDrawable(android.R.drawable.ic_menu_agenda);
            }

            categoryIcon.setImageDrawable(iconDrawable);
            categoryIcon.setColorFilter(cat.hidden ? 0xFFA6A6A6 : 0xFFFFFFFF);
            int iconSize = (int) (20 * density);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.leftMargin = dp12;
            categoryIcon.setLayoutParams(iconLp);
            row.addView(categoryIcon);

            TextView nameTv = new TextView(a);
            nameTv.setText(cat.name + (cat.hidden ? " (Hidden)" : ""));
            nameTv.setTextColor(0xFFFFFFFF);
            nameTv.setTextSize(15);
            nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
            textLp.leftMargin = dp12;
            nameTv.setLayoutParams(textLp);
            row.addView(nameTv);

            row.setFocusable(true);
            row.setClickable(true);
            setClickWithHaptic(row, v -> {
                selected[idx] = !selected[idx];
                checkBox.setChecked(selected[idx]);
            });

            box.addView(row);
        }

        ScrollView scroll = new ScrollView(a) {
            { setPadding(0, 0, dp8, 0); setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET); }
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int maxHeight = (int) (480 * density);
                int hMode = MeasureSpec.getMode(heightMeasureSpec);
                int hSize = MeasureSpec.getSize(heightMeasureSpec);
                if (hMode == MeasureSpec.UNSPECIFIED || hSize > maxHeight) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollbarThumbDrawable(createScrollThumb(a));
        scroll.addView(box);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialogBox.addView(scroll, scrollLp);

        AlertDialog d = showCustomDialog(a, dialogBox, fixedWidth);
        d.setOnDismissListener(dialogInterface -> {
            for (int i = 0; i < list.size(); i++) {
                Categories.Category cat = list.get(i);
                if (selected[i]) {
                    cat.members.add(pkg);
                    cat.exclusions.remove(pkg);
                } else {
                    cat.members.remove(pkg);
                    if (!cat.custom()) {
                        cat.exclusions.add(pkg);
                    }
                }
            }
            save(a, data, changed);
        });
    }

    private static void AppsPicker(Activity a, Categories data, Categories.Category category, Runnable changed) {
        try {
            List<?> apps = (List<?>) XposedHelpers.callMethod(a, "getMAllApp");
            TreeMap<String, String> names = new TreeMap<>();
            for (Object app : apps) {
                String pkg = HookEntry.pkg(app);
                String name = String.valueOf(XposedHelpers.callMethod(app, "getName"));
                names.put(pkg, name.isEmpty() ? pkg : name);
            }
            List<String> packages = new ArrayList<>(names.keySet());
            packages.sort(Comparator.comparing(p -> names.get(p), String.CASE_INSENSITIVE_ORDER));

            boolean[] checked = new boolean[packages.size()];
            Object targetFilter = null;
            if (!category.custom()) {
                try {
                    Object filter = XposedHelpers.callMethod(a, "getMFilter");
                    targetFilter = XposedHelpers.callMethod(filter, "getTargetFilter", category.id);
                    if (targetFilter == null && ("app".equalsIgnoreCase(category.id) || "apps".equalsIgnoreCase(category.id))) {
                        targetFilter = XposedHelpers.callMethod(filter, "getTargetFilter", "app");
                    }
                    if (targetFilter == null && ("game".equalsIgnoreCase(category.id) || "entertainment".equalsIgnoreCase(category.id))) {
                        targetFilter = XposedHelpers.callMethod(filter, "getTargetFilter", "game");
                    }
                } catch (Throwable ignored) {}
            }
            for (int i = 0; i < packages.size(); i++) {
                String pkg = packages.get(i);
                boolean isChecked = category.members.contains(pkg);
                if (!isChecked && !category.exclusions.contains(pkg) && targetFilter != null) {
                    try {
                        for (Object app : apps) {
                            if (pkg.equals(HookEntry.pkg(app))) {
                                isChecked = (Boolean) XposedHelpers.callMethod(targetFilter, "c", app);
                                break;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                checked[i] = isChecked;
            }

            int fixedWidth = dialogWidth(a);
            float density = a.getResources().getDisplayMetrics().density;
            int dp16 = (int) (16 * density);
            int dp12 = (int) (12 * density);
            int dp8 = (int) (8 * density);

            LinearLayout dialogBox = new LinearLayout(a);
            dialogBox.setOrientation(LinearLayout.VERTICAL);
            dialogBox.setPadding(dp16, dp16, dp8, dp16);
            dialogBox.setLayoutParams(new LinearLayout.LayoutParams(fixedWidth, -2));
            dialogBox.setMinimumWidth(fixedWidth);

            // Title
            TextView titleTv = new TextView(a);
            titleTv.setText(category.name + " - Apps");
            titleTv.setTextColor(0xFFFFFFFF);
            titleTv.setTextSize(18);
            titleTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            titleTv.setPadding(0, 0, 0, dp16);
            dialogBox.addView(titleTv);

            LinearLayout box = new LinearLayout(a);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setLayoutParams(new ScrollView.LayoutParams(-1, -2));

            for (int i = 0; i < packages.size(); i++) {
                final int idx = i;
                String pkg = packages.get(idx);
                String appName = names.get(pkg);

                LinearLayout row = new LinearLayout(a);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(hoverableRect(0xFF2C2C2E, 0xFF3A3A3C, 12f, a));
                row.setPadding(dp16, dp12, dp16, dp12);

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.bottomMargin = dp6_or_8(density, 6);
                row.setLayoutParams(rowLp);

                CheckBox checkBox = new CheckBox(a);
                checkBox.setChecked(checked[idx]);
                checkBox.setClickable(false);
                tintCheckBox(checkBox, 0x773e8bff);
                applyHaptic(checkBox);
                row.addView(checkBox);

                ImageView appIcon = new ImageView(a);
                Drawable iconDrawable = null;
                try {
                    iconDrawable = a.getPackageManager().getApplicationIcon(pkg);
                } catch (Throwable ignored) {
                    try {
                        iconDrawable = a.getPackageManager().getDefaultActivityIcon();
                    } catch (Throwable ignored2) {}
                }
                if (iconDrawable != null) {
                    appIcon.setImageDrawable(iconDrawable);
                }
                int iconSize = (int) (32 * density);
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconLp.leftMargin = dp12;
                appIcon.setLayoutParams(iconLp);
                appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(appIcon);

                LinearLayout textCol = new LinearLayout(a);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
                textLp.leftMargin = dp12;
                textCol.setLayoutParams(textLp);

                TextView nameTv = new TextView(a);
                nameTv.setText(appName);
                nameTv.setTextColor(0xFFFFFFFF);
                nameTv.setTextSize(15);
                nameTv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                textCol.addView(nameTv);

                TextView pkgTv = new TextView(a);
                pkgTv.setText(pkg);
                pkgTv.setTextColor(0xFFA6A6A6);
                pkgTv.setTextSize(12);
                pkgTv.setTypeface(android.graphics.Typeface.DEFAULT);
                textCol.addView(pkgTv);

                row.addView(textCol);

                row.setFocusable(true);
                row.setClickable(true);
                setClickWithHaptic(row, v -> {
                    checked[idx] = !checked[idx];
                    checkBox.setChecked(checked[idx]);
                });

                box.addView(row);
            }

            ScrollView scroll = new ScrollView(a) {
                { setPadding(0, 0, dp8, 0); setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET); }
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int maxHeight = (int) (480 * density);
                    int hMode = MeasureSpec.getMode(heightMeasureSpec);
                    int hSize = MeasureSpec.getSize(heightMeasureSpec);
                    if (hMode == MeasureSpec.UNSPECIFIED || hSize > maxHeight) {
                        heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            };
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            scroll.setFillViewport(true);
            scroll.setVerticalScrollbarThumbDrawable(createScrollThumb(a));
            scroll.addView(box);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialogBox.addView(scroll, scrollLp);

            AlertDialog d = showCustomDialog(a, dialogBox, fixedWidth);
            d.setOnDismissListener(dialogInterface -> {
                category.members.clear();
                category.exclusions.clear();
                for (int i = 0; i < packages.size(); i++) {
                    String pkg = packages.get(i);
                    if (checked[i]) {
                        category.members.add(pkg);
                    } else {
                        if (!category.custom()) {
                            category.exclusions.add(pkg);
                        }
                    }
                }
                save(a, data, changed);
                CategoriesManager(a, data, changed);
            });

        } catch (Throwable failure) {
            XposedBridge.log("PICO Categories: app picker unavailable " + failure);
            Toast.makeText(a, "Open Library's app list first", Toast.LENGTH_LONG).show();
        }
    }
}

 /* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko;

import org.mozilla.gecko.gfx.ImmutableViewportMetrics;
import org.mozilla.gecko.gfx.LayerView;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

class TextSelectionHandle extends ImageView implements View.OnTouchListener {
    private static final String LOGTAG = "GeckoTextSelectionHandle";

    private enum HandleType { START, MIDDLE, END }; 

    private final HandleType mHandleType;
    private final int mWidth;
    private final int mHeight;
    private final int mShadow;

    private int mLeft;
    private int mTop;
    private PointF mGeckoPoint;
    private int mTouchStartX;
    private int mTouchStartY;

    private RelativeLayout.LayoutParams mLayoutParams;

    TextSelectionHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextSelectionHandle);
        int handleType = a.getInt(R.styleable.TextSelectionHandle_handleType, 0x01);

        if (handleType == 0x01)
            mHandleType = HandleType.START;
        else if (handleType == 0x02)
            mHandleType = HandleType.MIDDLE;
        else
            mHandleType = HandleType.END;

        mGeckoPoint = new PointF(0.0f, 0.0f);

        mWidth = getResources().getDimensionPixelSize(R.dimen.text_selection_handle_width);
        mHeight = getResources().getDimensionPixelSize(R.dimen.text_selection_handle_height);
        mShadow = getResources().getDimensionPixelSize(R.dimen.text_selection_handle_shadow);
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mTouchStartX = Math.round(event.getX());
                mTouchStartY = Math.round(event.getY());
                break;
            }
            case MotionEvent.ACTION_UP: {
                mTouchStartX = 0;
                mTouchStartY = 0;

                // Reposition handles to line up with ends of selection
                JSONObject args = new JSONObject();
                try {
                    args.put("handleType", mHandleType.toString());
                } catch (Exception e) {
                    Log.e(LOGTAG, "Error building JSON arguments for TextSelection:Position");
                }
                GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("TextSelection:Position", args.toString()));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                move(Math.round(event.getX()), Math.round(event.getY()));
                break;
            }
        }
        return true;
    }

    private void move(int newX, int newY) {
        mLeft = mLeft + newX - mTouchStartX;
        mTop = mTop + newY - mTouchStartY;

        LayerView layerView = GeckoApp.mAppContext.getLayerView();
        if (layerView == null) {
            Log.e(LOGTAG, "Can't move selection because layerView is null");
            return;
        }
        // Send x coordinate on the right side of the start handle, left side of the end handle.
        float left = (float) mLeft;
        if (mHandleType.equals(HandleType.START))
            left +=  mWidth - mShadow;
        else if (mHandleType.equals(HandleType.MIDDLE))
            left +=  (float) ((mWidth - mShadow) / 2);
        else
            left += mShadow;

        PointF geckoPoint = new PointF(left, (float) mTop);
        geckoPoint = layerView.convertViewPointToLayerPoint(geckoPoint);

        JSONObject args = new JSONObject();
        try {
            args.put("handleType", mHandleType.toString());
            args.put("x", Math.round(geckoPoint.x));
            args.put("y", Math.round(geckoPoint.y));
        } catch (Exception e) {
            Log.e(LOGTAG, "Error building JSON arguments for TextSelection:Move");
        }
        GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("TextSelection:Move", args.toString()));

        setLayoutPosition();
    }

    void positionFromGecko(int left, int top) {
        LayerView layerView = GeckoApp.mAppContext.getLayerView();
        if (layerView == null) {
            Log.e(LOGTAG, "Can't position handle because layerView is null");
            return;
        }

        mGeckoPoint = new PointF((float) left, (float) top);
        ImmutableViewportMetrics metrics = layerView.getViewportMetrics();
        repositionWithViewport(metrics.viewportRectLeft, metrics.viewportRectTop, metrics.zoomFactor);
    }

    void repositionWithViewport(float x, float y, float zoom) {
        PointF viewPoint = new PointF((mGeckoPoint.x * zoom) - x,
                                      (mGeckoPoint.y * zoom) - y);

        mLeft = Math.round(viewPoint.x);
        if (mHandleType.equals(HandleType.START))
            mLeft -=  mWidth - mShadow;
        else if (mHandleType.equals(HandleType.MIDDLE))
            mLeft -=  (float) ((mWidth - mShadow) / 2);
        else
            mLeft -= mShadow;

        mTop = Math.round(viewPoint.y);

        setLayoutPosition();
    }

    private void setLayoutPosition() {
        if (mLayoutParams == null) {
            mLayoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
            // Set negative right/bottom margins so that the handles can be dragged outside of
            // the content area (if they are dragged to the left/top, the dyanmic margins set
            // below will take care of that).
            mLayoutParams.rightMargin = 0 - mWidth;
            mLayoutParams.bottomMargin = 0 - mHeight;
        }

        mLayoutParams.leftMargin = mLeft;
        mLayoutParams.topMargin = mTop;
        setLayoutParams(mLayoutParams);
    }
}

package ieor140;

import java.util.ArrayList;

import lejos.android.RCNavigationControl;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class Map extends View implements OnTouchListener {

    public Point point = new Point(0, 0);
    public ArrayList<Point> path = new ArrayList<Point>();
    public ArrayList<Point> obstacles = new ArrayList<Point>();
    public Point destination = new Point(0, 0);
    RCNavigationControl control;

    public Map(Context context) {
        super(context);
        this.setOnTouchListener(this);
    }

    public Map(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnTouchListener(this);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawColor(android.graphics.Color.CYAN);
        if (path.size() > 0) {
            for (int i = 0; i < path.size() - 1; i++) {
                drawRobotPath(canvas, path.get(i), path.get(i + 1));
            }
            drawRobotPosition(canvas, path.get(path.size() - 1));
        }
        if (obstacles.size() > 0) {
            for (int i = 0; i < obstacles.size(); i++) {
                drawObstacle(canvas, obstacles.get(i));
            }
        }
        drawDestination(canvas, destination);
    }

    public void drawRobotPath(Canvas canvas, Point startPoint, Point endPoint) {
        int startX = startPoint.x;
        int startY = startPoint.y;
        int stopX = endPoint.x;
        int stopY = endPoint.y;
        startX = convertX(startX);
        startY = convertY(startY);
        stopX = convertX(stopX);
        stopY = convertY(stopY);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(3);
        canvas.drawLine(startX, startY, stopX, stopY, paint);
    }

    public void drawRobotPosition(Canvas canvas, Point pointDraw) {
        int x = pointDraw.x;
        int y = pointDraw.y;
        x = convertX(x);
        y = convertY(y);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, 20, paint);
    }

    public void drawObstacle(Canvas canvas, Point pointDraw) {
        int x = pointDraw.x;
        int y = pointDraw.y;
        x = convertX(x);
        y = convertY(y);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawCircle(x, y, 5, paint);
    }

    public void drawDestination(Canvas canvas, Point pointDraw) {
        int x = pointDraw.x;
        int y = pointDraw.y;
        x = convertX(x);
        y = convertY(y);
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(x, y, 10, paint);
    }

    public int convertX(int x) {
        int multiplier = this.getWidth() / 480;
        x = x * multiplier + this.getWidth() / 2;
        return x;
    }

    public int convertY(int y) {
        float multiplier = this.getHeight() / 240.0f;
        y = this.getHeight() - (int) (y * multiplier);
        return y;
    }

    public int touchConvertX(int x) {
        x = x - this.getWidth() / 2;
        int multiplier = this.getWidth() / 480;
        x = x / multiplier;
        return x;
    }

    public int touchConvertY(int y) {
        y = this.getHeight() - y;
        float multiplier = this.getHeight() / 240.0f;
        y = (int) (y / multiplier);
        return y;
    }

    public void reDraw() {
        this.invalidate();
    }

    public void setControl(RCNavigationControl control) {
        this.control = control;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        x = touchConvertX(x);
        y = touchConvertY(y);
        destination.x = x;
        destination.y = y;
        control.setRobotDestination(x, y);
        return false;
    }
}
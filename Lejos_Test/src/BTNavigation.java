import lejos.nxt.Motor;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Pose;
import lejos.util.Delay;

/**
 * Main class on robot side: interprets messages received from Reader and
 * performs the appropriate action
 * 
 * @author duttasho
 */
public class BTNavigation implements CommListener {

    DifferentialPilot pilot = new DifferentialPilot(5.696, 5.6, 13.71, Motor.A, Motor.B, false);
    PoseProvider poseProv = new OdometryPoseProvider(pilot);
    Pose origin = new Pose(0, 0, 0);
    Navigator navigator = new Navigator(pilot, poseProv);
    Communicator communicator = new Communicator(this);
    boolean messageReceived = false;
    Header header;
    float[] data;
    boolean bit;

    /**
     * Sends location of robot to PC via communicator
     * 
     * @param p
     *            Pose of robot
     */
    public void sendLocation(Pose p) {
        float[] data = { p.getX(), p.getY(), p.getHeading(), 0 };
        communicator.sendData(Header.POSE, data, true);
    }

    /**
     * Notifies the robot of a new message being received by setting boolean
     * messageReceived to true
     * 
     * @param header
     *            Message header: contains Header enum
     * @param data
     *            data of message: array of 3 floats
     * @param bit
     *            boolean - not really used
     */
    public void newMessage(Header header, float[] data, boolean bit) {
        this.header = header;
        this.data = data;
        this.bit = bit;
        messageReceived = true;
        if (header == Header.STOP) {
            navigator.stop();
            navigator.clearPath();
        }
    }

    /**
     * Main method: calls go()
     * 
     * @param args
     */
    public static void main(String[] args) {

        BTNavigation myRobot = new BTNavigation();
        myRobot.go();
    }

    /**
     * Connects to PC, then when message received, performs action
     */
    public void go() {
        poseProv.setPose(origin);
        pilot.setAcceleration(1400);

        communicator.connect();
        while (true) {
            if (messageReceived) {
                messageReceived = false;
                switch (header) {
                case DESTINATION:
                    navigator.goTo(data[0], data[1]);
                    while (navigator.isMoving()) {
                        Delay.msDelay(200);
                        sendLocation(navigator.getPoseProvider().getPose());
                    }
                    break;
                case STOP:
                    navigator.stop();
                    break;
                case POSE:
                    Pose newPose = new Pose();
                    newPose.setLocation(data[0], data[1]);
                    newPose.setHeading(data[2]);
                    navigator.getPoseProvider().setPose(newPose);
                    break;
                case FIXPOSITION:
                    break;
                case ROTATE:
                    pilot.rotate(data[0]);
                    sendLocation(navigator.getPoseProvider().getPose());
                    break;
                case TRAVEL:
                    pilot.travel(data[0]);
                    while (pilot.isMoving()) {
                        Delay.msDelay(200);
                        sendLocation(navigator.getPoseProvider().getPose());
                    }
                    break;
                case MAP:
                    break;
                case BOMB:
                    break;

                }

            }

        }
    }
}

/*
 * Notes for present and future developments:
 * Commands:
 * Mouse click e.g. -> robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);   robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
 * - Mouse Left click: InputEvent.BUTTON1_DOWN_MASK
 * - Mouse Middle click: InputEvent.BUTTON2_DOWN_MASK
 * - Mouse Right click: InputEvent.BUTTON3_DOWN_MASK
 
 * Fingers. In the hand object we can read the fingers using hand.fingers().get(x) where "x" can be:
 * - 0 : thumb

 * - 1 : index
 * - 2 : middle
 * - 3 : ring
 * - 4 : pinky

 * If the Z vector of the hand's palm is positive it means it is pointing downward (scroll down). If negative he palm is pointing upward (scroll up). Range: [-1, 1].
 
 * How to reduce all icons and show the Desktop?
    Simulating (Win + D) in Windows:
        Robot keyHandler = null;
        try {
            keyHandler = new Robot();
        } catch (AWTException ex) {
            Logger.getLogger(SimpleListener.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(keyHandler == null)
            return;
        keyHandler.keyPress(KeyEvent.VK_WINDOWS);
        keyHandler.keyPress(KeyEvent.VK_D);
        keyHandler.keyRelease(KeyEvent.VK_WINDOWS);
        keyHandler.keyRelease(KeyEvent.VK_D);
 * 
 *  Simulating ( Ctrl + Alt + d ) in GNOME Linux
        keyHandler.keyPress(KeyEvent.VK_CTRL);
        keyHandler.keyPress(KeyEvent.VK_ALT);
        keyHandler.keyPress(KeyEvent.VK_D);
        keyHandler.keyRelease(KeyEvent.VK_CTRL);
        keyHandler.keyRelease(KeyEvent.VK_ALT);
        keyHandler.keyRelease(KeyEvent.VK_D);
 * 
 *  Simulating ( Command-Shift-Up Arrow ) in OS (Apple)
        keyHandler.keyPress(KeyEvent.VK_META);
        keyHandler.keyPress(KeyEvent.VK_SHIFT);
        keyHandler.keyPress(KeyEvent.VK_UP);
        keyHandler.keyRelease(KeyEvent.VK_META);
        keyHandler.keyRelease(KeyEvent.VK_SHIFT);
        keyHandler.keyRelease(KeyEvent.VK_UP);
 * 
 * 
 */
package leapmotionapp;

/**
 *
 * @author Marco Carlo Cavalazzi, born in Larino (Campobasso), Italy, on the 2nd of November, 1987.
 */
import java.io.IOException;
import com.leapmotion.leap.*;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.*;       // Used to control the Master volume of the PC.

class SimpleListener extends Listener {
    //True for Debugging
    boolean DEBUG = false;
    
    @Override
    public void onInit(Controller controller) {
        System.out.println("Initialized");
    }

    @Override
    public void onConnect(Controller controller) {
        System.out.println("Connected");
        controller.enableGesture(Gesture.Type.TYPE_SWIPE);
        controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
        //controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);   // To enable touchless touch feature
        controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
    }

    @Override
    public void onDisconnect(Controller controller) {
        //Note: not dispatched when running in a debugger.
        System.out.println("Disconnected");
    }
    
    @Override
    public void onExit(Controller controller) {
        System.out.println("Exited");
    }
    
    double maxTip = 0; double previousMaxTip = 0; boolean flagOverTip = false;
    boolean mouseLeftButtonPressed = false;     // These variables will tell us if the buttons have already been pressed or not.
    @Override
    public void onFrame(Controller controller) {
        // Local variables
        int time = 0;
        int smoothing = 1;
        
        // Get the most recent frame and report some basic information
        Frame frame = controller.frame();
        
        Robot robot = null; // This class is used to generate native system input events (like Mouse or Keyboard events).
        try {
            robot = new Robot(); // Used to move the mouse around
        } catch (AWTException ex) {
            Logger.getLogger(SimpleListener.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        // When there is at least one readable hand
        if(!frame.hands().isEmpty()){
            // Get the first finger in the list of fingers.
            Hand rightHand = frame.hands().rightmost();
            if(rightHand.isRight()){    // If we are actually looking at the right hand...
                Finger thumbFinger = rightHand.fingers().get(0);    // gets the thumb of the rightmost hand seen by the LEAP controller.
                Finger indexFinger = rightHand.fingers().get(1);    // gets the index finger of the rightmost hand seen by the LEAP controller.
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // Reading the monitor's width and height of the PC
                double monitorWidth = screenSize.getWidth();
                double monitorHeight = screenSize.getHeight();
                
                
                float yawOfThumbTip = thumbFinger.bone(Bone.Type.TYPE_DISTAL).direction().yaw();
                // Debugging statement
                System.out.println("thumb yaw: "+yawOfThumbTip);
                
                if(yawOfThumbTip < 0   &&  !mouseLeftButtonPressed){    // If the thumb's tip points toward the inside of the hand (toward the right) and the mouse left button has not been clicked yet...
                    System.out.println("CLICK");
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);     // Clicking the mouse's left button
                    mouseLeftButtonPressed = true;
                }else{
                    if(yawOfThumbTip > 0  &&  mouseLeftButtonPressed){  // If the mouse button has been clicked and noe he thumb's tip point away from the hand (to the left)...
                        System.out.println("RELEASE");
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);   // Releasing the mouse's left button
                        mouseLeftButtonPressed = false;
                    }
                }
                
                // stretching the fingers -> min diff x=-138.6243133544922  Max diff x=114.96267700195312   min diff y=-145.86447143554688   Max diff y=115.65048217773438
                // Moving normally        -> min diff x=-91.89445495605469  Max diff x=47.381954193115234   min diff y=-55.870391845703125   Max diff y=69.254638671875
                // Assuming min X difference = -70     max X difference = 50      (lower numbers will allow the User to not have to strech its fingers to point to the borders of the screen)
                //      and min Y difference = -70     max Y difference = 90      
                int minFingerRangeX = -70;
                int maxFingerRangeX = 50;
                int minFingerRangeY = -50;
                int maxFingerRangeY = 50;
                int fingerMaxWidth = Math.abs(minFingerRangeX) + Math.abs(maxFingerRangeX);   // Positive (e.g.: 160).
                int fingerMaxHeight = Math.abs(minFingerRangeY) + Math.abs(maxFingerRangeY);
                
                Vector indexFingerTip = indexFinger.tipPosition();  // Reading the position of the tip of the index finger in the LEAP controller's space
                Vector indexFingerBase = indexFinger.bone(Bone.Type.TYPE_METACARPAL).prevJoint();   // Reading the position of the metacarpal joint between the metacarp and the index finger in the LEAP controller's space
                double diffX = (indexFingerTip.getX()-indexFingerBase.getX()) + (Math.abs(minFingerRangeX));    // Adding "minFingerRange" to translate e.g. from [-70, 90] to [0, 160]
                double diffY = (indexFingerTip.getY()-indexFingerBase.getY()) + (Math.abs(minFingerRangeY));    // idem
                
                // Bounding the final results
                diffX = boundMe(diffX, 0, fingerMaxWidth);
                diffY = boundMe(diffY, 0, fingerMaxHeight);
                
                // Reading the position of the mouse in the screen
                Point mousePosition = MouseInfo.getPointerInfo().getLocation();
                double mouseX = mousePosition.getX();
                double mouseY = mousePosition.getY();
                
                // If the User is moving the mouse slowly is because he/she needs precision. In this case...
                // Using tipVelocity to reduce jitters when attempting to move slowly or hold the cursor steady.
                float indexTipVelocity = indexFingerTip.magnitude();  // Reading how fast the index finger is moving.
                if(indexTipVelocity <= 5){ return; }
                if(indexTipVelocity <= 25){
                    //System.out.println("small section");
                    
                    int foreseenMousePositionX = (int) ((diffX / fingerMaxWidth)*monitorWidth);
                    int foreseenMousePositionY = (int) (monitorHeight - ((diffY / fingerMaxHeight)*monitorHeight));

                    // From these values we want to define a square inside the screen (bounding the values to not exceed the monitor's dimensions).
                    int oneSixtythOfWidth = (int) (monitorWidth/30);   // We have to move back and forward from the foreseen position of 1/5 of the monitor's width.
                    int newLeftBorder = foreseenMousePositionX - oneSixtythOfWidth;
                    int newRightBorder = foreseenMousePositionX + oneSixtythOfWidth;
                    int newBottomBorder = foreseenMousePositionY - oneSixtythOfWidth;
                    int newTopBorder = foreseenMousePositionY + oneSixtythOfWidth;

                    // Consistency checks (limiting the values of the variables)
                    newLeftBorder = boundMe(newLeftBorder, 0, (int) monitorWidth);
                    newRightBorder = boundMe(newRightBorder, 0, (int) monitorWidth);
                    newBottomBorder = boundMe(newBottomBorder, 0, (int) monitorHeight);
                    newTopBorder = boundMe(newTopBorder, 0, (int) monitorHeight);

                    // Calculating the width and height of the secion of the monitor
                    int newMonitorWidth = newRightBorder - newLeftBorder;
                    int newMonitorHeight = newTopBorder - newBottomBorder;

                    // Calculating the fnal position of the mouse in the smaller screen
                    foreseenMousePositionX = (int) ((diffX / fingerMaxWidth)*newMonitorWidth);      // Will be something between 0 and newMonitorWidth
                    foreseenMousePositionY = (int) (newMonitorHeight - ((diffY / fingerMaxHeight)*newMonitorHeight));  // Will be something between 0 and newMonitorHeight

                    // Converting the values obtained to move the mouse in the real (bigger) monitor
                    int mouseFinalPositionX = newLeftBorder + foreseenMousePositionX;
                    int mouseFinalPositionY = newBottomBorder + foreseenMousePositionY;    // We need this trick because otherwise it would move in "reverse" mode.

                    // The number of steps, since it is a very delicate moving of the mouse, will be equl of the max number of pixels that differ from the starting point to the end point between the Xs and the Ys.
                    int numSteps = (int) Math.sqrt(Math.pow(mouseFinalPositionX - mouseX, 2)  +  Math.pow(mouseFinalPositionY - mouseY, 2));
                    mouseGlide((int)mouseX, (int)mouseY, mouseFinalPositionX, mouseFinalPositionY, 50, numSteps, robot);    // time in ms, number of steps, robot

                }else{
                    
                    if(indexTipVelocity > 25  && indexTipVelocity < 50){   // controlling to avoid jitters before controlling if the movement is slow
                        // We want to move the mouse in a smaller screen. We limit the movements in a section of the actual screen
                        // in a square around the foreseen mouse final position.
                        int foreseenMousePositionX = (int) ((diffX / fingerMaxWidth)*monitorWidth);
                        int foreseenMousePositionY = (int) (monitorHeight - ((diffY / fingerMaxHeight)*monitorHeight));
                        
                        // From these values we want to define a square inside the screen (bounding the values to not exceed the monitor's dimensions).
                        int oneThirtythOfWidth = (int) (monitorWidth/30);   // We have to move back and forward from the foreseen position of 1/5 of the monitor's width.
                        int newLeftBorder = foreseenMousePositionX - oneThirtythOfWidth;
                        int newRightBorder = foreseenMousePositionX + oneThirtythOfWidth;
                        int newBottomBorder = foreseenMousePositionY - oneThirtythOfWidth;
                        int newTopBorder = foreseenMousePositionY + oneThirtythOfWidth;
                        
                        // Consistency checks (limiting the values of the variables)
                        newLeftBorder = boundMe(newLeftBorder, 0, (int) monitorWidth);
                        newRightBorder = boundMe(newRightBorder, 0, (int) monitorWidth);
                        newBottomBorder = boundMe(newBottomBorder, 0, (int) monitorHeight);
                        newTopBorder = boundMe(newTopBorder, 0, (int) monitorHeight);

                        // Calculating the width and height of the secion of the monitor
                        int newMonitorWidth = newRightBorder - newLeftBorder;
                        int newMonitorHeight = newTopBorder - newBottomBorder;

                        // Calculating the fnal position of the mouse in the smaller screen
                        foreseenMousePositionX = (int) ((diffX / fingerMaxWidth)*newMonitorWidth);      // Will be something between 0 and newMonitorWidth
                        foreseenMousePositionY = (int) (newMonitorHeight - ((diffY / fingerMaxHeight)*newMonitorHeight));  // Will be something between 0 and newMonitorHeight

                        // Converting the values obtained to move the mouse in the real (bigger) monitor
                        int mouseFinalPositionX = newLeftBorder + foreseenMousePositionX;
                        int mouseFinalPositionY = newBottomBorder + foreseenMousePositionY;    // We need this trick because otherwise it would move in "reverse" mode.
                        
                        // The number of steps, since it is a very delicate moving of the mouse, will be equl of the max number of pixels that differ from the starting point to the end point between the Xs and the Ys.
                        int numSteps = (int) Math.sqrt(Math.pow(mouseFinalPositionX - mouseX, 2)  +  Math.pow(mouseFinalPositionY - mouseY, 2));
                        mouseGlide((int)mouseX, (int)mouseY, mouseFinalPositionX, mouseFinalPositionY, 50 , numSteps/7, robot);    // time in ms, number of steps, robot
                        
                    }else{
                        // Since the finger can do only a circle around its base next to the metacarp, we have to adapt the circle shape to the rectangle one of the screen.
                        // Re-bounding the final results
                        diffX = boundMe(diffX, 0, fingerMaxWidth);
                        diffY = boundMe(diffY, 0, fingerMaxHeight);

                        int mouseFinalPositionX = (int) ((diffX / fingerMaxWidth)*monitorWidth);
                        int mouseFinalPositionY = (int) (monitorHeight - ((diffY / fingerMaxHeight)*monitorHeight));    // We need this trick because otherwise it would move in "reverse" mode.

                        // The number of steps, since it is a very delicate moving of the mouse, will be equl of the max number of pixels that differ from the starting point to the end point between the Xs and the Ys.
                        int numSteps = (int) Math.sqrt(Math.pow(mouseFinalPositionX - mouseX, 2)  +  Math.pow(mouseFinalPositionY - mouseY, 2));
                        mouseGlide((int)mouseX, (int)mouseY, mouseFinalPositionX, mouseFinalPositionY, 50, numSteps/8, robot);    // time in ms, number of steps, robot

                    }
                    
                    // End of mouse control section
                }
                
            }
            
        }
        
    }
    
    
    public int boundMe(double val, int min, int max){
        if(val < min){ return min; }
        else{ if(val > max){ return max; }}
        return (int)val;
    }
    
    // Credit to Andy Zhang via Stack Overflow.
    /* Input: 
     * - starting coordinates
     * - final coordinates
     * - time: the amount of time the operation has to take to move the mouse
     * - number of steps to do in the movement
     * - robot
     */
    @SuppressWarnings({"CallToPrintStackTrace", "SleepWhileInLoop"})
    public static void mouseGlide(int x1, int y1, int x2, int y2, int time, int n, Robot r) {
        try {
            double dx = (x2 - x1) / ((double) n);
            double dy = (y2 - y1) / ((double) n);
            double dt = time / ((double) n);
            for (int step = 1; step <= n; step++) {
                Thread.sleep((int) dt);
                r.mouseMove((int) (x1 + dx * step), (int) (y1 + dy * step));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static double inBounds(double i){
            if(i > 1){
                    return 1;
            } else if (i < 0){
                    return 0;
            } else{
                    return i;
            }
    }
}

// MAIN FUNCTION
public class LeapMotionApp {
    public static void main(String[] args) {
        // Create a sample listener and controller
        SimpleListener listener = new SimpleListener();
        Controller controller = new Controller();

        // Have the sample listener receive events from the controller
        controller.addListener(listener);

        // Keep this process running until Enter is pressed
        System.out.println("Press Enter to quit...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Remove the sample listener when done
        controller.removeListener(listener);
    }
}

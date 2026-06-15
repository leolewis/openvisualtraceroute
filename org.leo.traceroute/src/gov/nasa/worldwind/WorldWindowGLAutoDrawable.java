package gov.nasa.worldwind;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.AWTGLAutoDrawable;
import com.jogamp.opengl.util.texture.TextureIO;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.ScreenCreditController;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.dashboard.DashboardController;
import org.leo.traceroute.install.Env;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.swing.Timer;

public class WorldWindowGLAutoDrawable extends WorldWindowImpl implements WorldWindowGLDrawable, GLEventListener {
    public static final long DEFAULT_VIEW_STOP_TIME = 1000L;
    private GLAutoDrawable drawable;
    private DashboardController dashboard;
    private boolean shuttingDown = false;
    private Timer redrawTimer;
    private boolean firstInit = true;
    protected long viewStopTime = 1000L;
    protected long lastViewID;
    protected ScheduledFuture viewRefreshTask;
    protected boolean enableGpuCacheReinitialization = true;

    public WorldWindowGLAutoDrawable() {
        SceneController var1 = this.getSceneController();
        if (var1 != null) {
            var1.addPropertyChangeListener(this);
        }

    }

    public long getViewStopTime() {
        return this.viewStopTime;
    }

    public void setViewStopTime(long var1) {
        this.viewStopTime = var1;
    }

    public void initDrawable(GLAutoDrawable var1) {
        if (var1 == null) {
            String var2 = Logging.getMessage("nullValue.DrawableIsNull");
            Logging.logger().severe(var2);
            throw new IllegalArgumentException(var2);
        } else {
            this.drawable = var1;
            this.drawable.setAutoSwapBufferMode(false);
            this.drawable.addGLEventListener(this);
        }
    }

    public boolean isEnableGpuCacheReinitialization() {
        return this.enableGpuCacheReinitialization;
    }

    public void setEnableGpuCacheReinitialization(boolean var1) {
        this.enableGpuCacheReinitialization = var1;
    }

    public void initGpuResourceCache(GpuResourceCache var1) {
        if (var1 == null) {
            String var2 = Logging.getMessage("nullValue.GpuResourceCacheIsNull");
            Logging.logger().severe(var2);
            throw new IllegalArgumentException(var2);
        } else {
            this.setGpuResourceCache(var1);
        }
    }

    public void endInitialization() {
        this.initializeCreditsController();
        this.dashboard = new DashboardController(this, (Component)this.drawable);
    }

    protected void initializeCreditsController() {
        new ScreenCreditController((WorldWindow)this.drawable);
    }

    public void shutdown() {
        this.shuttingDown = true;
        this.redrawNow();
    }

    protected void doShutdown() {
        super.shutdown();
        this.drawable.removeGLEventListener(this);
        if (this.dashboard != null) {
            this.dashboard.dispose();
        }

        if (this.viewRefreshTask != null) {
            this.viewRefreshTask.cancel(false);
        }

        this.shuttingDown = false;
    }

    public void propertyChange(PropertyChangeEvent var1) {
        if (var1 == null) {
            String var2 = Logging.getMessage("nullValue.PropertyChangeEventIsNull");
            Logging.logger().severe(var2);
            throw new IllegalArgumentException(var2);
        } else {
            this.redraw();
        }
    }

    public GLContext getContext() {
        return this.drawable.getContext();
    }

    protected boolean isGLContextCompatible(GLContext var1) {
        return var1 != null && var1.isGL2();
    }

    protected String[] getRequiredOglFunctions() {
        return new String[]{"glActiveTexture", "glClientActiveTexture"};
    }

    protected String[] getRequiredOglExtensions() {
        return new String[0];
    }

    public void init(GLAutoDrawable var1) {
        if (!this.isGLContextCompatible(var1.getContext())) {
            String var2 = Logging.getMessage("WorldWindowGLAutoDrawable.IncompatibleGLContext", new Object[]{var1.getContext()});
            this.callRenderingExceptionListeners(new WWAbsentRequirementException(var2));
        }

        for(String var5 : this.getRequiredOglFunctions()) {
            if (!var1.getGL().isFunctionAvailable(var5)) {
                this.callRenderingExceptionListeners(new WWAbsentRequirementException(var5 + " not available"));
            }
        }

        for(String var10 : this.getRequiredOglExtensions()) {
            if (!var1.getGL().isExtensionAvailable(var10)) {
                this.callRenderingExceptionListeners(new WWAbsentRequirementException(var10 + " not available"));
            }
        }

        if (this.firstInit) {
            this.firstInit = false;
        } else if (this.enableGpuCacheReinitialization) {
            this.reinitialize(var1);
        }

        TextureIO.setTexRectEnabled(false);
    }

    protected void reinitialize(GLAutoDrawable var1) {
        if (this.getGpuResourceCache() != null) {
            this.getGpuResourceCache().clear();
        }

        this.getSceneController().reinitialize();
    }

    public void dispose(GLAutoDrawable var1) {
    }

    public void display(GLAutoDrawable var1) {
        if (this.shuttingDown) {
            try {
                this.doShutdown();
            } catch (Exception var13) {
                Logging.logger().log(Level.SEVERE, Logging.getMessage("WorldWindowGLCanvas.ExceptionWhileShuttingDownWorldWindow"), var13);
            }

        } else {
            try {
                SceneController var2 = this.getSceneController();
                if (var2 == null) {
                    String var17 = Logging.getMessage("WorldWindowGLCanvas.ScnCntrllerNullOnRepaint");
                    Logging.logger().severe(var17);
                    throw new IllegalStateException(var17);
                }

                this.checkForViewChange();
                Position var3 = this.getCurrentPosition();
                PickedObject var4 = this.getCurrentSelection();
                PickedObjectList var5 = this.getCurrentBoxSelection();

                try {
                    this.callRenderingListeners(new RenderingEvent(this.drawable, "gov.nasa.worldwind.RenderingEvent.BeforeRendering"));
                } catch (Exception var15) {
                    Logging.logger().log(Level.SEVERE, Logging.getMessage("WorldWindowGLAutoDrawable.ExceptionDuringGLEventListenerDisplay"), var15);
                }

                int var6 = this.doDisplay();
                if (var6 > 0 && this.redrawTimer == null) {
                    this.redrawTimer = new Timer(var6, new ActionListener() {
                        public void actionPerformed(ActionEvent var1) {
                            WorldWindowGLAutoDrawable.this.redraw();
                            WorldWindowGLAutoDrawable.this.redrawTimer = null;
                        }
                    });
                    this.redrawTimer.setRepeats(false);
                    this.redrawTimer.start();
                }

                try {
                    this.callRenderingListeners(new RenderingEvent(this.drawable, "gov.nasa.worldwind.RenderingEvent.BeforeBufferSwap"));
                } catch (Exception var14) {
                    Logging.logger().log(Level.SEVERE, Logging.getMessage("WorldWindowGLAutoDrawable.ExceptionDuringGLEventListenerDisplay"), var14);
                }

                this.doSwapBuffers(this.drawable);
                Double var7 = var2.getFrameTime();
                if (var7 != null) {
                    this.setValue("gov.nasa.worldwind.perfstat.FrameTime", var7);
                }

                Double var8 = var2.getFramesPerSecond();
                if (var8 != null) {
                    this.setValue("gov.nasa.worldwind.perfstat.FrameRate", var8);
                }

                Collection<Throwable> var9 = var2.getRenderingExceptions();
                if (var9 != null) {
                    for (Throwable var11 : var9) {
                        if (var11 != null) {
                            this.callRenderingExceptionListeners(var11);
                        }
                    }
                }

                this.callRenderingListeners(new RenderingEvent(this.drawable, "gov.nasa.worldwind.RenderingEvent.AfterBufferSwap"));
                Position var18 = this.getCurrentPosition();
                if (var3 != null || var18 != null) {
                    if (var3 != null && var18 != null) {
                        if (!var3.equals(var18)) {
                            this.callPositionListeners(new PositionEvent(this.drawable, var2.getPickPoint(), var3, var18));
                        }
                    } else {
                        this.callPositionListeners(new PositionEvent(this.drawable, var2.getPickPoint(), var3, var18));
                    }
                }

                PickedObject var19 = this.getCurrentSelection();
                if (var4 != null || var19 != null) {
                    this.callSelectListeners(new SelectEvent(this.drawable, "gov.nasa.worldwind.SelectEvent.Rollover", var2.getPickPoint(), var2.getPickedObjectList()));
                }

                PickedObjectList var12 = this.getCurrentBoxSelection();
                if (var5 != null || var12 != null) {
                    this.callSelectListeners(new SelectEvent(this.drawable, "gov.nasa.worldwind.SelectEvent.BoxRollover", var2.getPickRectangle(), var2.getObjectsInPickRectangle()));
                }
            } catch (Exception var16) {
                Logging.logger().log(Level.SEVERE, Logging.getMessage("WorldWindowGLCanvas.ExceptionAttemptingRepaintWorldWindow"), var16);
            }

        }
    }

    protected void checkForViewChange() {
        long var1 = this.getView().getViewStateID();
        if (var1 != this.lastViewID) {
            this.lastViewID = var1;
            this.scheduleViewStopTask(this.getViewStopTime());
        }

    }

    protected int doDisplay() {
        return this.getSceneController().repaint();
    }

    protected void doSwapBuffers(GLAutoDrawable var1) {
        var1.swapBuffers();
    }

    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h)
    {
        if (Env.INSTANCE.getOs() == Env.OS.win) {
            double dpiScalingFactor = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
            GL2 gl = this.drawable.getGL().getGL2(); // change this as needed
            w = (int) (w * dpiScalingFactor);
            h = (int) (h * dpiScalingFactor);
            gl.glViewport(0, 0, w, h);
            ((Component) glAutoDrawable).setMinimumSize(new Dimension(0, 0));
            ((Component) glAutoDrawable).setMinimumSize(new Dimension(0, 0));
        } else {
            ((Component) glAutoDrawable).setMinimumSize(new Dimension(0, 0));
        }
    }
    public void redraw() {
        if (this.drawable != null) {
            ((AWTGLAutoDrawable)this.drawable).repaint();
        }

    }

    public void redrawNow() {
        if (this.drawable != null) {
            this.drawable.display();
        }

    }

    protected void scheduleViewStopTask(long var1) {
        Runnable var3 = new Runnable() {
            public void run() {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        WorldWindowGLAutoDrawable.this.onMessage(new Message("gov.nasa.worldwind.View.ViewStopped", WorldWindowGLAutoDrawable.this));
                    }
                });
            }
        };
        if (this.viewRefreshTask != null) {
            this.viewRefreshTask.cancel(false);
        }

        this.viewRefreshTask = WorldWind.getScheduledTaskService().addScheduledTask(var3, var1, TimeUnit.MILLISECONDS);
    }

    public void onMessage(Message var1) {
        Model var2 = this.getModel();
        if (var2 != null) {
            var2.onMessage(var1);
        }

    }
}

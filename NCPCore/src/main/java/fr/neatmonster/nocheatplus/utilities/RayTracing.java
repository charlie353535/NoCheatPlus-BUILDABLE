package fr.neatmonster.nocheatplus.utilities;

import org.bukkit.Location;

/**
 * Ray tracing for block coordinates with entry point offsets.
 * @author mc_dev
 *
 */
public abstract class RayTracing {

    //	/** End point coordinates (from, to) */
    //	protected double x0, y0, z0, x1, y1, z1;

    //	/** Total distance between end points. */
    //	protected double d;

    /** Distance per axis. */
    protected double dX, dY, dZ;

    /** Current block. */
    protected int blockX, blockY, blockZ;

    /** End block. */
    protected int endBlockX, endBlockY, endBlockZ;

    /** Offset within current block. */
    protected double oX, oY, oZ;

    /** Current "time" in [0..1]. */
    protected double t = Double.MIN_VALUE;

    /** Tolerance for time, for checking the abort condition: 1.0 - t <= tol . */
    protected double tol = 0.0;

    /** Counting the number of steps. Step is incremented before calling step(), and is 0 after set(...).  Checking this from within step means to get the current step number, checking after loop gets the number of steps done. */
    protected int step = 0;

    /** Maximum steps that will be done. */
    private int maxSteps = Integer.MAX_VALUE;

    public RayTracing(double x0, double y0, double z0, double x1, double y1, double z1) {
        set(x0, y0, z0, x1, y1, z1);
    }

    public RayTracing() {
        set(0, 0, 0, 0, 0, 0);
    }

    /**
     * After this calling loop is possible.
     * @param x0
     * @param y0
     * @param z0
     * @param x1
     * @param y1
     * @param z1
     */
    public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
        //		this.x0 = x0;
        //		this.y0 = y0;
        //		this.z0 = z0;
        //		this.x1 = x1;
        //		this.y1 = y1;
        //		this.z1 = z1;
        //		d = CheckUtils.distance(x0, y0, z0, x1, y1, z1);
        dX = x1 - x0;
        dY = y1 - y0;
        dZ = z1 - z0;
        blockX = Location.locToBlock(x0);
        blockY = Location.locToBlock(y0);
        blockZ = Location.locToBlock(z0);
        endBlockX = Location.locToBlock(x1);
        endBlockY = Location.locToBlock(y1);
        endBlockZ = Location.locToBlock(z1);
        oX = (double) (x0 - blockX);
        oY = (double) (y0 - blockY);
        oZ = (double) (z0 - blockZ);
        t = 0.0;
        step = 0;
    }

    private static final double tDiff(final double dTotal, final double offset, final int block, final int endBlock) {
        if (dTotal > 0.0) {
            if (offset >= 1.0) {
                // Static block change (e.g. diagonal move).
                return 0.0; // block == endBlock ? Double.MAX_VALUE : 0.0;
            } else {
                return (1.0 - offset) / dTotal; 
            }
        }
        else if (dTotal < 0.0) {
            if (offset <= 0.0) {
                // Static block change (e.g. diagonal move).
                return 0.0; //block == endBlock ? Double.MAX_VALUE : 0.0;
            } else {
                return offset / -dTotal;
            }
        }
        else {
            return Double.MAX_VALUE;
        }
    }

    /**
     * Loop through blocks.
     */
    public void loop() {
        // TODO: Might intercept 0 dist ?

        // Time to block edge.
        double tX, tY, tZ, tMin;
        boolean changed;
        while (1.0 - t > tol) {
            // Determine smallest time to block edge.
            tX = tDiff(dX, oX, blockX, endBlockX);
            tY = tDiff(dY, oY, blockY, endBlockY);
            tZ = tDiff(dZ, oZ, blockZ, endBlockZ);
            tMin = Math.min(tX,  Math.min(tY, tZ));
            if (tMin == Double.MAX_VALUE) {
                // All differences are 0 (no progress).
                if (step < 1) {
                    // Allow one step.
                    tMin = 0.0;
                } else {
                    break;
                }
            }
            if (t + tMin > 1.0) {
                // Set to the remaining distance (does trigger).
                tMin = 1.0 - t;
            }
            // Call step with appropriate arguments.
            step ++;
            if (!step(blockX, blockY, blockZ, oX, oY, oZ, tMin)) {
                break;
            }
            if (t + tMin >= 1.0 - tol) { // && isEndBlock()) {
                break;
            }
            // Advance (add to t etc.).
            changed = false;
            oX = Math.min(1.0, Math.max(0.0, oX + tMin * dX));
            oY = Math.min(1.0, Math.max(0.0, oY + tMin * dY));
            oZ = Math.min(1.0, Math.max(0.0, oZ + tMin * dZ));
            // TODO: Consider Heuristic change of the checking order for dy > 0 vs. dy < 0. 
            // x
            if (tX == tMin && blockX != endBlockX) {
                if (dX < 0) {
                    oX = 1.0;
                    blockX --;
                    changed = true;
                }
                else if (dX > 0) {
                    oX = 0.0;
                    blockX ++;
                    changed = true;
                }
            }
            if (!changed) {
                // y
                if (tY == tMin && blockY != endBlockY) {
                    if (dY < 0) {
                        oY = 1.0;
                        blockY --;
                        changed = true;
                    }
                    else if (dY > 0) {
                        oY = 0.0;
                        blockY ++;
                        changed = true;
                    }
                }
                if (!changed) {
                    // z
                    if (tZ == tMin && blockZ != endBlockZ) {
                        if (dZ < 0) {
                            oZ = 1.0;
                            blockZ --;
                            changed = true;
                        }
                        else if (dZ > 0) {
                            oZ = 0.0;
                            blockZ ++;
                            changed = true;
                        }
                    }
                }
            }
            t += tMin;
            if (!changed || step >= maxSteps) {
                break;
            }
        }
        // TODO: Catch special case with going beyond coordinates.
    }

    /**
     * Indicate if a collision appeared during loop(). This must be overridden to return a result other than false.
     * @return 
     */
    public boolean collides() {
        return false;
    }

    public boolean isEndBlock() {
        return blockX == endBlockX && blockY == endBlockY && blockZ == endBlockZ;
    }

    /**
     * This is for external use. The field step will be incremented before
     * step(...) is called, thus checking it from within step means to get the
     * current step number, checking after loop gets the number of steps done.
     * 
     * @return
     */
    public int getStepsDone() {
        return step;
    }

    /**
     * Get the maximal number of steps that loop will do.
     * @return
     */
    public int getMaxSteps() {
        return maxSteps;
    }

    /**
     * Set the maximal number of steps that loop will do.
     * @return
     */
    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    /**
     * One step in the loop.
     * @return If to continue loop.
     */
    protected abstract boolean step(int blockX, int blockY, int blockZ, double oX, double oY, double oZ, double dT);

}

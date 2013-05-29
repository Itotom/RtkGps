package ru0xdc.rtklib;

import java.io.File;

import javax.annotation.Nonnull;

import ru0xdc.rtkgps.MainActivity;


public class RtkServer {

    /* mObject is used by native code, do not remove or rename */
    private long mObject;

    public final static int RECEIVER_ROVER = 0;
    public final static int RECEIVER_BASE = 1;
    public final static int RECEIVER_EPHEM = 2;

    /**
     * Status: {@link RtkServerStreamStatus.STATE_CLOSE},
     * {@link RtkServerStreamStatus.STATE_WAIT},
     * {@link RtkServerStreamStatus.STATE_ACTIVE}.
     */
    private int mStatus;

    private final Solution.SolutionBuffer mSolutionBuffer;

    private RtkServerSettings mSettings;


    public RtkServer() {
        _create();
        mStatus = RtkServerStreamStatus.STATE_CLOSE;
        mSolutionBuffer = new Solution.SolutionBuffer();
        mSettings = new RtkServerSettings();
    }

    public boolean start() {
        final boolean started;

        started = _rtksvrstart(
                mSettings.getServerCycleMs(),
                mSettings.getBufferSize(),
                mSettings.getStreamTypes(),
                mSettings.getStreamPaths(),
                mSettings.getInputStreamFormats(),
                mSettings.getNavMessageSelect(),
                mSettings.getInputStreamCommandsAtStartup(),
                mSettings.getInputStreamReceiverOption(),
                mSettings.getNmeaRequestCycle(),
                mSettings.getNmeaRequestType(),
                mSettings.getTransmittedPos().getValues(),
                mSettings.getProcessingOptions().getNative(),
                mSettings.getSolutionOptions1().getNative(),
                mSettings.getSolutionOptions2().getNative()
                );

        mStatus = started ? RtkServerStreamStatus.STATE_WAIT : RtkServerStreamStatus.STATE_ERROR;
        return started;
    }

    public void stop() {
        _stop();
        mStatus = RtkServerStreamStatus.STATE_CLOSE;
    }

    public RtkServerStreamStatus getStreamStatus(RtkServerStreamStatus status) {
        if (status == null) status = new RtkServerStreamStatus();
        _getStreamStatus(status);
        // XXX
        if ((mStatus == RtkServerStreamStatus.STATE_WAIT)
                && (status.getInputRoverStatus() > RtkServerStreamStatus.STATE_WAIT))
            mStatus = RtkServerStreamStatus.STATE_ACTIVE;
        return status;
    }

    public RtkServerObservationStatus getBaseObservationStatus(RtkServerObservationStatus status) {
        return getObservationStatus(RtkServer.RECEIVER_BASE, status);
    }

    public RtkServerObservationStatus getRoverObservationStatus(RtkServerObservationStatus status) {
        return getObservationStatus(RtkServer.RECEIVER_ROVER, status);
    }

    public RtkServerObservationStatus getEphemObservationStatus(RtkServerObservationStatus status) {
        return getObservationStatus(RtkServer.RECEIVER_EPHEM, status);
    }

    /**
     * @return Status: {@link RtkServerStreamStatus.STATE_CLOSE},
     * {@link RtkServerStreamStatus.STATE_WAIT},
     * {@link RtkServerStreamStatus.STATE_ACTIVE}.
     */
    public int getStatus(){
        return mStatus;
    }

    public Solution getLastSolution() {
        _readSolutionBuffer(mSolutionBuffer);
        return mSolutionBuffer.getLastSolution();
    }

    public RtkControlResult getRtkStatus(RtkControlResult dst) {
        if (dst == null) dst = new RtkControlResult();
        _getRtkStatus(dst);
        return dst;
    }

    public void setServerSettings(RtkServerSettings settings) {
        if (mStatus != RtkServerStreamStatus.STATE_CLOSE) throw new IllegalStateException();
        mSettings.setValues(settings);
    }

    public final RtkServerSettings getServerSettings() {
        return mSettings;
    }

    private RtkServerObservationStatus getObservationStatus(int receiver, RtkServerObservationStatus status) {
        if (status == null) status = new RtkServerObservationStatus(receiver);
        _getObservationStatus(receiver, status.getNative());
        return status;
    }

    public void sendStartupCommands(int stream) {
        switch (stream) {
        case RECEIVER_ROVER:
            _writeCommands(mSettings.getInputRover().getCommandsAtStartup(), null, null);
            break;
        case RECEIVER_BASE:
            _writeCommands(null,mSettings.getInputBase().getCommandsAtStartup(),  null);
            break;
        case RECEIVER_EPHEM:
            _writeCommands(null,null,mSettings.getInputCorrection().getCommandsAtStartup());
            break;
        }
    }

    @Nonnull
    // getPathInStorageDirectory() is used by native code, do not remove or rename
    public static String getPathInStorageDirectory(@Nonnull String filename) {
        return new File(MainActivity.getFileStorageDirectory(), filename).getAbsolutePath();
    }


    @Override
    protected void finalize() throws Throwable {
        _destroy();
        super.finalize();
    }

    private native void _stop();

    private native void _create();

    private native void _destroy();

    private native void _getStreamStatus(RtkServerStreamStatus status);

    private native void _getObservationStatus(int receiver, RtkServerObservationStatus.Native status);

    private native void _getRtkStatus(RtkControlResult dst);

    private native void _readSolutionBuffer(Solution.SolutionBuffer dst);

    private native void _writeCommands(String roverCmd, String baseCmd, String correctionsCmd);

    /**
     * Start rtk server thread
     * @param cycle     server cycle (ms)
     * @param buffsize  input buffer size (bytes)
     * @param strs      stream types (STR_???)
     *					types[0]=input stream rover
     *					types[1]=input stream base station
     *					types[2]=input stream correction
     *					types[3]=output stream solution 1
     * 					types[4]=output stream solution 2
     *					types[5]=log stream rover
     *				 	types[6]=log stream base station
     *					types[7]=log stream correction
     * @param paths		input stream paths
     * @param format	input stream formats (STRFMT_???)
     * 					format[0]=input stream rover
     * 					format[1]=input stream base station
     * 					format[2]=input stream correction
     * @param navsel	ephemeris select (0:all,1:rover,2:base,3:corr)
     * @param cmds		input stream start commands
     * 					cmds[0]=input stream rover (NULL: no command)
     * 					cmds[1]=input stream base (NULL: no command)
     * 					cmds[2]=input stream corr (NULL: no command)
     * @param rcvopts	receiver options
     * 					rcvopt[0]=receiver option rover
     * 					rcvopt[1]=receiver option base
     * 					rcvopt[2]=receiver option corr
     * @param nmeacycle nmea request cycle (ms) (0:no request)
     * @param nmeareq   nmea request type (0:no,1:base pos,2:single sol)
     * @param nmeapos	transmitted nmea position (ecef) (m)
     * @param procopt   processing options
     * @param solopt_1  solution 1 options
     * @oaram solopt_2   solution 2 options
     */
    private native boolean _rtksvrstart(
            int cycle,
            int buffsize,
            int strs[],
            String paths[],
            int format[],
            int navsel,
            String cmds[],
            String rcvopts[],
            int nmeacycle,
            int nmeareq,
            double nmeapos[],
            ProcessingOptions.Native procopt,
            SolutionOptions.Native solopt_1,
            SolutionOptions.Native solopt_2
            );

}

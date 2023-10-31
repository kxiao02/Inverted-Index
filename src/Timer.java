class Timer {

    final long begin;
    long lastRecord;
    Timer() {
        begin = System.currentTimeMillis();
        lastRecord = begin;
    }

    double update(int type) {
        long now = System.currentTimeMillis();
        double time;
        if (type == 0) {
            time = (now - lastRecord) / 1000.0;
            lastRecord = now;
        } else {
            time = (now - begin) / 1000.0;
            lastRecord = now;
        }
        return time;
    }
}

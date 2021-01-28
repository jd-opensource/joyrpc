package io.joyrpc.cluster;

import io.joyrpc.util.StateTransition;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 分片状态机
 */
public class ShardStateTransition implements StateTransition {

    protected static final AtomicReferenceFieldUpdater<ShardStateTransition, Shard.ShardState> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(ShardStateTransition.class, Shard.ShardState.class, "state");

    protected Shard.ShardState state;

    public ShardStateTransition() {
        this(Shard.ShardState.INITIAL);
    }

    public ShardStateTransition(Shard.ShardState state) {
        this.state = state;
    }

    @Override
    public boolean isOpening() {
        return state == Shard.ShardState.CONNECTING;
    }

    @Override
    public boolean isOpened() {
        return state == Shard.ShardState.CONNECTED || state == Shard.ShardState.WEAK;
    }

    @Override
    public boolean isClosing() {
        return state == Shard.ShardState.CLOSING;
    }

    public boolean isDisconnect() {
        return state == Shard.ShardState.DISCONNECT;
    }

    @Override
    public boolean isClosed() {
        return state == Shard.ShardState.DISCONNECT || state == Shard.ShardState.INITIAL;
    }

    @Override
    public boolean isClose() {
        return state == Shard.ShardState.DISCONNECT || state == Shard.ShardState.INITIAL || state == Shard.ShardState.CLOSING;
    }

    @Override
    public boolean isOpen() {
        return state == Shard.ShardState.CONNECTED || state == Shard.ShardState.WEAK || state == Shard.ShardState.CONNECTING;
    }

    @Override
    public String name() {
        return state.name();
    }

    /**
     * 进入初始化状态
     *
     * @return 初始化标识
     */
    public int tryInitial() {
        return state.initial(this::setState) ? SUCCESS : FAILED;
    }

    /**
     * 进入后续状态
     *
     * @return 成功标识
     */
    public int tryCandidate() {
        return state.candidate(this::setState) ? SUCCESS : FAILED;
    }

    /**
     * 进入断开连接状态
     *
     * @return 成功标识
     */
    public int tryDisconnect() {
        return state.disconnect(this::setState) ? SUCCESS : FAILED;
    }

    @Override
    public int tryOpening() {
        return state.connecting(this::setState) ? SUCCESS : FAILED;
    }

    @Override
    public int tryOpened() {
        return state.connected(this::setState) ? SUCCESS : FAILED;
    }

    /**
     * 进入虚弱状态
     *
     * @return 虚弱标识
     */
    public int tryWeak() {
        return state.weak(this::setState) ? SUCCESS : FAILED;
    }

    @Override
    public int tryClosing() {
        return state.closing(this::setState) ? SUCCESS : FAILED;
    }

    @Override
    public int tryClosed() {
        return state.initial(this::setState) ? SUCCESS : FAILED;
    }

    @Override
    public void toClosed() {
        state = Shard.ShardState.DISCONNECT;
    }

    /**
     * 设置状态
     *
     * @param source 原状态
     * @param target 目标状态
     * @return 成功标识
     */
    protected boolean setState(final Shard.ShardState source, final Shard.ShardState target) {
        return STATE_UPDATER.compareAndSet(this, source, target);
    }
}

package com.asdflj.ae2thing.client.me;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.client.gui.widget.IGuiMonitor;
import com.asdflj.ae2thing.common.Config;
import com.asdflj.ae2thing.util.Ae2ReflectClient;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.widgets.IScrollSource;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.me.ItemRepo;
import appeng.me.cache.ItemFlowGridCache.FlowRate;

public class AdvItemRepo extends ItemRepo implements Runnable {

    private static final int SIZE = 1;
    private static final BlockingQueue<Runnable> IN = new LinkedBlockingQueue<>();
    private static final ThreadPoolExecutor pool = new ThreadPoolExecutor(
        SIZE,
        SIZE,
        60,
        TimeUnit.SECONDS,
        IN,
        r -> new Thread(r, "AE2 Thing repo sort thread"));

    protected final ArrayList<IAEStack<?>> view = Ae2ReflectClient.getView(this);
    protected final IItemList<IAEStack<?>> list = Ae2ReflectClient.getList(this);

    protected AdvItemRepo repo;
    protected final Set<IAEStack<?>> cache = Collections.synchronizedSet(new HashSet<>());
    protected IGuiMonitor gui;
    private static final Lock lock = new ReentrantLock();
    private final AtomicBoolean updateQueued = new AtomicBoolean();
    private final AtomicBoolean updateRequested = new AtomicBoolean();

    public AdvItemRepo(IScrollSource src, ISortSource sortSrc) {
        super(src, sortSrc);
    }

    public AdvItemRepo(ISortSource sortSrc) {
        this(null, sortSrc);
    }

    public Lock getLock() {
        return lock;
    }

    public void setCache(IGuiMonitor gui) {
        if (Config.updateViewThread) {
            this.repo = new AdvItemRepo(gui);
            this.repo.setPowered(true);
            this.gui = gui;
        }
    }

    public boolean hasCache() {
        return repo != null;
    }

    @Override
    public void setSearchString(@Nonnull String searchString) {
        if (this.hasCache()) {
            repo.setSearchString(searchString);
        }
        super.setSearchString(searchString);
    }

    @Override
    public void setViewCell(ItemStack[] list) {
        if (this.hasCache()) {
            repo.setViewCell(list);
        }
        super.setViewCell(list);
    }

    /**
     * Flow rates drive the FLOWING view mode, which the background repo is the one to actually evaluate, so they must
     * reach it as well as the outer instance.
     */
    @Override
    public void updateFlowRates(final Map<IAEStack<?>, FlowRate> rates) {
        if (this.hasCache()) {
            repo.updateFlowRates(rates);
        }
        super.updateFlowRates(rates);
    }

    @Override
    @Deprecated
    public void postUpdate(IAEItemStack is) {
        this.postUpdate((IAEStack<?>) is);
    }

    @Override
    public void postUpdate(IAEStack<?> is) {
        if (this.hasCache()) {
            lock.lock();
            this.cache.remove(is);
            this.cache.add(is);
            lock.unlock();
        }
        super.postUpdate(is);
    }

    @Override
    public void updateView() {
        if (this.hasCache()) {
            this.updateRequested.set(true);
            this.scheduleViewUpdate();
        } else {
            super.updateView();
        }
    }

    private void scheduleViewUpdate() {
        if (!this.updateQueued.compareAndSet(false, true)) return;
        pool.execute(() -> {
            try {
                while (this.updateRequested.getAndSet(false)) {
                    this.run();
                }
            } finally {
                this.updateQueued.set(false);
                if (this.updateRequested.get()) this.scheduleViewUpdate();
            }
        });
    }

    @Override
    public void run() {
        try {
            lock.lock();
            for (IAEStack<?> is : this.cache) {
                this.repo.postUpdate(is);
            }
            this.cache.clear();
        } finally {
            lock.unlock();
        }
        this.repo.updateView();
        try {
            lock.lock();
            this.view.clear();
            this.view.ensureCapacity(this.repo.view.size());
            this.view.addAll(this.repo.view);
        } finally {
            lock.unlock();
        }
        if (this.gui != null) Minecraft.getMinecraft()
            .func_152344_a(this.gui::setScrollBar);
    }

    @Override
    public void setPaused(boolean paused) {
        if (hasCache() && this.repo instanceof IDisplayRepoExtend dre) {
            dre.setAdvRepoPause(paused);
            if (!paused) this.updateView();
        } else {
            super.setPaused(paused);
        }
    }
}

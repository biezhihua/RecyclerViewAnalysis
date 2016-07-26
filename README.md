
#RecyclerView

#  基础概念

|   专业术语         |   -------------   |
|   ---------       |   -------------   |
|   Adapter(适配器)        |   RecyclerView.Adapter的子类,负责提供很多视图(views),用以表示数据集合中的数据 |
|   Position(位置)        |   一个数据项在Adapter内的位置 |
|   Index(索引)           |   调用getChildAt(int)时,获取指定位置绑定的子视图的索引位置,和Position是对照物.
|   Binding(绑定过程)      |   在Adapter内,用一个视图(view)在一个位置(Position)展示数据的过程 |   
|   Recycle(view)(回收)   |   之前用于显示适配器(Adapter)特定位置数据所绑定的视图,可以被放置在一个高速缓存中以便稍后再次显示相同类型的数据.这通过跳过初始布局填充以大幅度提高性能. |
|   Scrap(view)(废弃)     |   一个子视图(view)暂时和布局分离,那么其就是废弃视图.如果一个废弃的视图,没有要求重新绑定,或者被适配器修改,那么这个视图就被认为是脏的,脏视图没有办法被RecyclerView重复使用.|
|   Dirty(view)(脏)       |   如果视图是脏视图,那么其必须被Adapter重新绑定后才能够被展示. |

在RecyclerView中的位置:
介绍RecyclerView,必须提到RecyclerView.Adapter和RecyclerView.LayoutManager,其能在布局的过程中检测到数据集的的变化,以及动画造成的适配器的改变.这有助于提升性能,因为所有视图的绑定都发生在同一时间,应该尽力避免不必要的绑定.
基于这个原因,RecyclerView提供了两种不同类型的和位置相关的方法:
|   ---------       |   -------------   |
|   layout position |   一个条目在布局计算获中取的最新位置,这个位置是从布局管理器(LayoutManager)的角度提供的 |
|   adapter position|   在适配器(adapter)中一个条目的位置,这个位置是从适配器(adapter)的角度提供的 |

这个两个位置在除了调用adapter.notify*方法和计算更新的布局之外是相同的.

想要获取一个布局位置,可以通过`getLayoutPosition`或者`findViewHolderForLayoutPosition(int)`方法.获取的位置和用户目前在屏幕上看到的是一致的.
想要获取一个适配器位置,可以通过'getAdapterPosition()'或者`findViewHolderForAdapterPosition(int)`方法.当你需要去更新适配器的位置,即使他们尚未映射到布局上.
例如,如果您要访问的条目在适配器上ViewHolder点击事件,你应该使用getAdapterPosition（）.
要注意的是,如果调用notifyDataSetChanged(),并且新的布局尚未计算,这些方法可能不能够计算适配器位置.对于这个原因,你应该小心处理NO_POSITION或空的结果.

一般来说,当你要写一个RecyclerView.LayoutManager时,你几乎总是需要使用布局位置(layout position),当要写一个RecyclerView.Adapter,你应该使用适配器位置(adapter position). 


职责:

RecyclerView.Adapter用于处理数据集与ItemView的绑定关系
RecyclerView.LayoutManager用于测量并布局ItemView

# 测量 & 布局

RecyclerView将它的measure与layout过程委托给了RecyclerView.LayoutManager来处理,并且,它对子控件的measure及layout过程是逐个处理的,也就是说,执行完成一个子控件的measure及layout过程再去执行下一个.
关于这点,现在认识一下AutoMeasure 

mAutoMeasure决定布局是否由RecyclerView测量还是由LayoutManager来处理测量自身的工作.

如果它要支持WRAP_CONTENT,这个值通常为true.如果这个值为false, 通常要自定义的测量逻辑,并覆盖LayoutManager.onMeasure(int,int)来实现您的自定义测量的逻辑.

其实自动测量是一个方便的机制,布局管理轻松地包裹自己的内容或处理由RecyclerView的父类提供的各种测量规格.
在onMeasure(int,int)的执行过程会调用onLayoutChildren(RecyclerView.Recycler,RecyclerView.State),然后计算子视图(view)的尺寸.而且会支持RecyclerView的所有的动画功能.

自动测量的原理如下:

1. 应该调用LayoutManager的setAutoMeasureEnable(true)来启动自动测量.这样所有的框架布局管理器都会使用自动测量.
2. 当RecyclerView的onMeasure()被调用时,如果父类所给的测量规格是EXACTLY的,RecyclerView只会调用LayoutManager的onMeasure()方法,而且内部使用的是defaultOnMeasure()默认测量方法.
3. 如果测量规则不是EXACTLY的,则会在onMeasure()中开始布局的处理过程.
此时,它会挂起所有的适配器更新,决定是否运行预测布局.
如果决定要布局会调用onLayoutChildren()与State.isPreLayout为true.在此阶段,getWidth()和getHeight()仍会返回RecyclerView的宽度和高度.
4. 处理完预测情况后,RecyclerView将State.isMeasuring设置为true,State.isPreLayout位false,并调用onLayoutChildren(),开始布局view为最终状态.
此时通过LayoutManager可以调用getHeight()/getHeightMode()访问RecyclerView的测量规格.
5. 经过布局计算后,RecyclerView通过setMeasuredDimensionFromChildren()方法设置子视图测量宽度和高度(其中包括孩子们的边框+padding).在布局管理器中我们还可以覆盖
setMeasuredDimension()方法,来处理不同的情况.例如:GridLayoutManager,如果是垂直的,有3列,但只有2项,仍应测量其宽度以适应3个项目,而不是2的情况.

也就是说,如果需要支持WRAP_CONTENT,那么子控件的measure及layout就会提前在RecyclerView的测量方法中执行完成,需要先确定了子控件的大小及位置后,再由此设置RecyclerView的大小；
如果是其它情况(测量模式为EXACTLY),子控件的measure及layout过程就会延迟至RecyclerView的layout过程（RecyclerView.onLayout()）中执行.

了解完上述之后,理解onMeasure()也就简单了:

```java
protected void onMeasure(int widthSpec, int heightSpec) {
    // ... 
    
    // 自动测量
    if (mLayout.mAutoMeasure) {
    
        // 精准大小
        // ... 
        dispatchLayoutStep2();

        // ... 
    } else {
        //...
        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
    }
}

void defaultOnMeasure(int widthSpec, int heightSpec) {
    // 计算默认宽高
    setMeasuredDimension(width, height);
}

```

无论是否AutoMeasure,都会走入defaultOnMeasure()以用来确定一个RecyclerView的最小宽高,当是非精准大小时,会执行dispatchLayoutStep2()方法,dispatchLayoutStep2()方法中会调用onLayoutChildren()将布局工作转交给布局管理器.

下面以`LinearLayoutManager`为例:

在onLayoutChildren()方法开头的说明中,也描述了布局的算法过程：

1. 解析布局方向,确定mLayoutFromEnd的值.默认的布局方向是从end到start,如果调用LayoutManager.setReverseLayout(true),那么布局方向就是从start到end.
2. 检查孩子,确定锚点信息(偏移量和item位置),(updateAnchorInfoForLayout()),如果是垂直布局,就是获取RecyclerView的PaddingTop的值
3. 如果mLayoutFromEnd = true; 从底部的锚点偏移量开始向上填充数据
4. 如果mLayoutFromEnd = false; 从顶部的锚点偏移量开始向下填充数据

## 更新锚点信息

```java
private void updateAnchorInfoForLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                                           AnchorInfo anchorInfo) {
    // 根据待决定的数据更新锚点信息   
    // 根据孩子更新锚点信息
        
    // 计算锚点坐标
    anchorInfo.assignCoordinateFromPadding();
    // 
    anchorInfo.mPosition = mStackFromEnd ? state.getItemCount() - 1 : 0;
}
```

## 填充数据

```java

int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
        RecyclerView.State state, boolean stopOnFocusable) {
    ...
    int remainingSpace = layoutState.mAvailable + layoutState.mExtra;
    LayoutChunkResult layoutChunkResult = new LayoutChunkResult();
    
    // 是否还有数据
    while (...&&layoutState.hasMore(state)) {
        ...
        
        // 填充数据
        layoutChunk(recycler, state, layoutState, layoutChunkResult);

        ...
        if (...) {
            layoutState.mAvailable -= layoutChunkResult.mConsumed;
            remainingSpace -= layoutChunkResult.mConsumed;
        }
        if (layoutState.mScrollingOffset != LayoutState.SCOLLING_OFFSET_NaN) {
            layoutState.mScrollingOffset += layoutChunkResult.mConsumed;
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutState(recycler, layoutState);
        }
    }
    ...
}
```

在来看一下layoutChunk方法

```java
void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
        LayoutState layoutState, LayoutChunkResult result) {
    
    // 从废弃的View列表中或是Recycler中获取可用的view
    View view = layoutState.next(recycler);
    
    // 添加View到RecyclerView中,最终调用的是View.addView()方法
    LayoutParams params = (LayoutParams) view.getLayoutParams();
    if (layoutState.mScrapList == null) {
        if (mShouldReverseLayout == (layoutState.mLayoutDirection
                == LayoutState.LAYOUT_START)) {
            // 当前布局方向是LAYOUT_END,从底部添加view
            addView(view);
        } else {
            // 当前布局方向是LAYOUT_START,从顶部添加view
            addView(view, 0);
        }
    } else {
        if (mShouldReverseLayout == (layoutState.mLayoutDirection
                == LayoutState.LAYOUT_START)) {
            addDisappearingView(view);
        } else {
            addDisappearingView(view, 0);
        }
    }
    
    // 根据padding和decoration来测量孩子
    measureChildWithMargins(view, 0, 0);
    
    // 根据测量结果和外边距来在RecyclerView中布局view
    layoutDecorated(view, left + params.leftMargin, top + params.topMargin,
            right - params.rightMargin, bottom - params.bottomMargin); 
}
```

layoutChunk做的事情:
1. 获取下一位置view元素.      (layoutState.next(recycler))
2. 将其添加到RecyclerView中   (addView)
3. 测量view的孩子             (measureChildWithMargins(view))
4. 结合装饰(mDecorInsets)和padding以及margin来布局view的位置.

至此,在RecyclerView的measure及layout阶段,填充ItemView的算法为：向父容器增加子控件,测量子控件大小,布局子控件,布局锚点向当前布局方向平移子控件大小,重复上诉步骤至RecyclerView可绘制空间消耗完毕或子控件已全部填充. 

这样所有的子控件的measure和layout过程就完成了.回到RecyclerView的onMeasure()方法,执行mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec)这行代码的作用就是根据子控件的大小,设置RecyclerView的大小.至此,RecyclerView的measure和layout实际上已经完成了.


```java
@Override
protected void onLayout(boolean changed, int l, int t, int r, int b) {
    TraceCompat.beginSection(TRACE_ON_LAYOUT_TAG);
    dispatchLayout();
    TraceCompat.endSection();
    mFirstLayoutComplete = true;
}

void dispatchLayout() {
    mState.mIsMeasuring = false;
    if (mState.mLayoutStep == State.STEP_START) {
        dispatchLayoutStep1();
        mLayout.setExactMeasureSpecsFrom(this);
        dispatchLayoutStep2();
    } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != getWidth() ||
            mLayout.getHeight() != getHeight()) {
        // First 2 steps are done in onMeasure but looks like we have to run again due to
        // changed size.
        mLayout.setExactMeasureSpecsFrom(this);
        dispatchLayoutStep2();
    } else {
        // always make sure we sync them (to ensure mode is exact)
        mLayout.setExactMeasureSpecsFrom(this);
    }
    dispatchLayoutStep3();
}
```

可以看出,这里也会执行measure以及layout过程.结合onMeasure()方法对skipMeasure的判断,如果要支持WRAP_CONTENT,那么
子控件的measure和layout过程就会提前在onMeasure()中执行完成,也就是说,先确定子控件大小及位置后,再设置RecyclerView的大小.
如果是EXACTLY,子控件的measure和layout就会延迟到RecyclerView的onLayout()方法中.

# 绘制

RecyclerView中onDraw()的绘制过程主要体现在item装饰的绘制.

先来看一下ItemDecoration类,主要有onDraw()/onDrawOver()/getItemOffsets()三个方法.其允许我们给item视图添加特殊的绘图和布局,用于绘制的突出显示,分组,分割等。

```java
public static abstract class ItemDecoration {

    // 在绘制时绘制
    public void onDraw(Canvas c, RecyclerView parent, State state) {
        onDraw(c, parent);
    }
    @Deprecated
    public void onDraw(Canvas c, RecyclerView parent) {
    }
    // 在绘制后绘制
    public void onDrawOver(Canvas c, RecyclerView parent, State state) {
        onDrawOver(c, parent);
    }
    @Deprecated
    public void onDrawOver(Canvas c, RecyclerView parent) {
    }
    @Deprecated
    public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
        outRect.set(0, 0, 0, 0);
    }
    // 设置item偏移量
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
        getItemOffsets(outRect, ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition(),
                parent);
    }
}
```

我们分别看一下RecyclerView中在那些地方调用了这三个方法,来加深理解.

```java

// 调用ItemDecoration#onDrawOver()
@Override
public void draw(Canvas c) {
    super.draw(c);

    final int count = mItemDecorations.size();
    for (int i = 0; i < count; i++) {
        mItemDecorations.get(i).onDrawOver(c, this, mState);
    }
    
    // ... 
}

// 调用ItemDecoration#onDraw()
@Override
public void onDraw(Canvas c) {
    super.onDraw(c);

    final int count = mItemDecorations.size();
    for (int i = 0; i < count; i++) {
        mItemDecorations.get(i).onDraw(c, this, mState);
    }
}

// 调用ItemDecoration#getItemOffsets()
public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();

    final Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
    widthUsed += insets.left + insets.right;
    heightUsed += insets.top + insets.bottom;

    final int widthSpec = ...
    final int heightSpec = ...
    if (shouldMeasureChild(child, widthSpec, heightSpec, lp)) {
        child.measure(widthSpec, heightSpec);
    }
}
Rect getItemDecorInsetsForChild(View child) {
    ...
    final Rect insets = lp.mDecorInsets;
    insets.set(0, 0, 0, 0);
    final int decorCount = mItemDecorations.size();
    for (int i = 0; i < decorCount; i++) {
        mTempRect.set(0, 0, 0, 0);
        // 调用
        mItemDecorations.get(i).getItemOffsets(mTempRect, child, this, mState);
        insets.left += mTempRect.left;
        insets.top += mTempRect.top;
        insets.right += mTempRect.right;
        insets.bottom += mTempRect.bottom;
    }
    lp.mInsetsDirty = false;
    return insets;
}
```

# 事件分发

说到RecyclerView触摸事件的分发,就必须要看看onInterceptTouchEvent()和onTouchEvent()方法.
其实了解Android事件分发机制的同学应该都清楚,事件分发无非是自身对事件的消耗或者是孩子是事件的消耗,以及都不消耗的情况下事件的回传.
而RecyclerView的事件分发机制也不例外,无非是是实现了更多自己的逻辑.
在RecyclerView处理自己的逻辑之前,通过OnItemTouchListener类为开发者提供了拦截事件的机会,这对于想实现自己的手势的操作非常有用.

```java
public static interface OnItemTouchListener {
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e);
    // 只有onInterceptTouchEvent()返回true,onTouchEvent()才会被调用.
    public void onTouchEvent(RecyclerView rv, MotionEvent e);
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept);
}
```

```java
 @Override
public boolean onInterceptTouchEvent(MotionEvent e) {
    //  冷冻布局
    if (mLayoutFrozen) {
        // When layout is frozen,  RV does not intercept the motion event.
        // A child view e.g. a button may still get the click.
        return false;
    }
    // 为开发者提供拦截事件的机会.
    if (dispatchOnItemTouchIntercept(e)) {
        cancelTouch();
        return true;
    }
    // ...
}

 @Override
public boolean onTouchEvent(MotionEvent e) {
    if (mLayoutFrozen || mIgnoreMotionEventTillDown) {
        return false;
    }
    // 为开发者提供拦截事件的机会.
    if (dispatchOnItemTouch(e)) {
        cancelTouch();
        return true;
    }
    // ...
}
```
dispatchOnItemTouchIntercept()和dispatchOnItemTouch()方法也比较简单,值得注意的是,由于触摸事件只能由一个listener来消耗,
所以用mActiveOnItemTouchListener变量来表示当前消耗事件的监听器,如果该监听器消耗了DOWN事件,那么该触摸事件剩下的事件就归该监听器消耗.

# 滑动

```java
@Override
public boolean onTouchEvent(MotionEvent e) {
    
    
    // ... 省略N多代码
    
    switch (action) {
        case MotionEvent.ACTION_DOWN: {
        
            // 记录按下位置
            mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
            mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
        } break;

         case MotionEvent.ACTION_MOVE: {
            final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
            final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
            final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);
            
            // 计算出手指移动距离
            int dx = mLastTouchX - x;
            int dy = mLastTouchY - y;

            // 与阀值做比较，并设置为SCROLL_STATE_DRAGGING状态
            if (mScrollState != SCROLL_STATE_DRAGGING) {
                boolean startScroll = false;
                if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                    if (dx > 0) {
                        dx -= mTouchSlop;
                    } else {
                        dx += mTouchSlop;
                    }
                    startScroll = true;
                }
                if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                    if (dy > 0) {
                        dy -= mTouchSlop;
                    } else {
                        dy += mTouchSlop;
                    }
                    startScroll = true;
                }
                if (startScroll) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    setScrollState(SCROLL_STATE_DRAGGING);
                }
            }

            // 调用scrollByInternal()方法，滑动RecyclerView
            if (mScrollState == SCROLL_STATE_DRAGGING) {
                mLastTouchX = x - mScrollOffset[0];
                mLastTouchY = y - mScrollOffset[1];

                if (scrollByInternal(
                        canScrollHorizontally ? dx : 0,
                        canScrollVertically ? dy : 0,
                        vtev)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } break;

        case MotionEvent.ACTION_UP: {
            mVelocityTracker.addMovement(vtev);
            eventAddedToVelocityTracker = true;
            // 计算滑动速率
            mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
            // 计算X轴最后速率
            final float xvel = canScrollHorizontally ? -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
            // 计算Y轴最后速率
            final float yvel = canScrollVertically ? -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;\
            // 滑动
            if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                setScrollState(SCROLL_STATE_IDLE);
            }
            resetTouch();
        } break;

        case MotionEvent.ACTION_CANCEL: {
            cancelTouch();
        } break;
    }
        
    // ... 省略代码

    return true;
}
```

当RecyclerView接收到ACTION_MOVE事件后，会先计算出手指移动距离（dy），并与滑动阀值（mTouchSlop）比较，当大于此阀值时将滑动状态设置为SCROLL_STATE_DRAGGING，而后调用scrollByInternal()方法，使RecyclerView滑动，这样RecyclerView的滑动的第一阶段scroll就完成了；
当接收到ACTION_UP事件时，会根据之前的滑动距离与时间计算出一个初速度yvel，这步计算是由VelocityTracker实现的，然后再以此初速度，调用方法fling()，完成RecyclerView滑动的第二阶段fling。
显然滑动过程中关键的方法就2个：scrollByInternal()与fling()。


以LinearLayout为例，最后会进入到scrollHorizontallyBy() scrollVerticallyBy()方法,最后都会走入到scrollBy()方法中。

```java
 boolean scrollByInternal(int x, int y, MotionEvent ev) {
    //... 省略代码
    
        if (x != 0) {
            consumedX = mLayout.scrollHorizontallyBy(x, mRecycler, mState);
            unconsumedX = x - consumedX;
        }
        if (y != 0) {
            consumedY = mLayout.scrollVerticallyBy(y, mRecycler, mState);
            unconsumedY = y - consumedY;
        }
        
    // ... 省略代码
    return consumedX != 0 || consumedY != 0;
}
```

```java
@Override
public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
        RecyclerView.State state) {
    // ...
    return scrollBy(dy, recycler, state);
}
```

在滚动过程中，不仅涉及到RecyclerView内容位置的改变，也涉及到数据的显示，以及Item的回收。所以可以看到scrollHorizontallyBy()方法中
将移动距离，回收者(Recycler)和状态对象都传入了scrollBy()中。

```java
int scrollBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
    if (getChildCount() == 0 || dy == 0) {
        return 0;
    }
    mLayoutState.mRecycle = true;
    ensureLayoutState();
    
    // 填充数据的方向，例如：从上向下滑动(dy<0)，填充数据的位置是列表顶部(LAYOUT_START = -1)，如果是从下向上滑动(dy>0)，填充数据的位置就是列表底部(LAYOUT_END = 1)
    final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
    final int absDy = Math.abs(dy);
    
    // 更新布局状态
    updateLayoutState(layoutDirection, absDy, true, state);
    
    // 根据布局状态填充数据
    final int consumed = mLayoutState.mScrollingOffset + fill(recycler, mLayoutState, state, false);
    if (consumed < 0) {
        return 0;
    }
    
    // 计算偏移量，一般来说 scrolled = dy
    final int scrolled = absDy > consumed ? layoutDirection * consumed : dy;
    
    // 如果是从下向上滑动，那么孩子的位置就要向上滚动位移(-方向)；如果是从上向下滑动，那么孩子的位置就要向下滚动位移(+方向)。
    mOrientationHelper.offsetChildren(-scrolled);
    mLayoutState.mLastScrollDelta = scrolled;
    return scrolled;
}
```


说完scrollBy()，再来看看 fling(int velocityX, int velocityY) 方法：

```java
// velocityX - 每秒的像素初始水平速度
// velocityY - 每秒的像素初始垂直速度
public boolean fling(int velocityX, int velocityY) {
        
        final boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        final boolean canScrollVertical = mLayout.canScrollVertically();

        // 不能滑动或者小于最小的速率(50 = ViewConfiguration.MINIMUM_FLING_VELOCITY)
        if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
         // 不能滑动或者小于最小的速率(50 = ViewConfiguration.MINIMUM_FLING_VELOCITY)
        if (!canScrollVertical || Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }
        if (velocityX == 0 && velocityY == 0) {
            // If we don't have any velocity, return false
            return false;
        }

        if (!dispatchNestedPreFling(velocityX, velocityY)) {
            final boolean canScroll = canScrollHorizontal || canScrollVertical;
            dispatchNestedFling(velocityX, velocityY, canScroll);

            if (canScroll) {
                velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
                velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
                //  继续滑动
                mViewFlinger.fling(velocityX, velocityY);
                return true;
            }
        }
        return false;
    }
```

来看一下fling()方法，其内部是借助Scroller实现的，我们都知道在View中借助Scroller实现弹性滑动还需要View的computeScroll()方法，
并在其中调用mScroller.computeScrollOffset()方法，然后在使用scrollTo()方法移动view，而在RecyclerVie中，并没有借助computeScroller()方法，而是使用postAnimation();

```java
public void fling(int velocityX, int velocityY) {
    setScrollState(SCROLL_STATE_SETTLING);
    mLastFlingX = mLastFlingY = 0;
    mScroller.fling(0, 0, velocityX, velocityY,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
    postOnAnimation();
}

void postOnAnimation() {
    if (mEatRunOnAnimationRequest) {
        mReSchedulePostAnimationCallback = true;
    } else {
        removeCallbacks(this);
        ViewCompat.postOnAnimation(RecyclerView.this, this);
    }
}
```

其中ViewCompat.postOnAnimation()的第二个参数Runnable,我们来看一下ViewFlinger.run()方法，虽然附加的逻辑多了一些，但是去除之后，还是能一眼就看清楚执行逻辑的 run() -> computeScrollOffset() -> scrollBy() -> postOnAnimation().

```java

@Override
public void run() {
    // ... 省略代码

    // 计算偏移
    if (scroller.computeScrollOffset()) {
    
        final int x = scroller.getCurrX();
        final int y = scroller.getCurrY();
        final int dx = x - mLastFlingX;
        final int dy = y - mLastFlingY;
        
        // .. 省略代码
        
        if (mAdapter != null) {
            // 最终调用scrollBy()方法
            if (dx != 0) {
                hresult = mLayout.scrollHorizontallyBy(dx, mRecycler, mState);
                overscrollX = dx - hresult;
            }
            // 最终调用scrollBy()方法
            if (dy != 0) {
                vresult = mLayout.scrollVerticallyBy(dy, mRecycler, mState);
                overscrollY = dy - vresult;
            }
            
            // ...
        }
        
        // ... 省略代码
        
        if (scroller.isFinished() || !fullyConsumedAny) {
            setScrollState(SCROLL_STATE_IDLE); // setting state to idle will stop this.
        } else {
            // 重新post，运行run()方法。
            postOnAnimation();
        }
    }
    // .. 省略代码
}
```

分析完上面两个方法，发现他们的最终实现其实是一样的，都会走到scrollBy()方法中。

# Recycler

Recycler的作用就是重用ItemView。在填充ItemView的时候，ItemView是从它获取的；滑出屏幕的ItemView是由它回收的。对于不同状态的ItemView存储在了不同的集合中，scrapped、cached、exCached、recycler，当然这些集合并不是都定义在同一个类里。 

在layoutChunk方法中，有行代码layoutState.next(recycler)，它的作用自然就是获取ItemView，我们进入这个方法查看，最终它会调用到RecyclerView.Recycler.getViewForPosition()方法.

```java
View getViewForPosition(int position, boolean dryRun) {
    // ... 省略代码
    
    // 0) If there is a changed scrap, try to find from there
    if (mState.isPreLayout()) {
        holder = getChangedScrapViewForPosition(position);
        fromScrap = holder != null;
    }
    
    // 1) Find from scrap by position
    if (holder == null) {
        holder = getScrapViewForPosition(position, INVALID_TYPE, dryRun);
        // ... 省略代码
    }
    if (holder == null) {
    
        final int offsetPosition = mAdapterHelper.findPositionOffset(position);
        // ... 省略代码
        
        // 获取item类型
        final int type = mAdapter.getItemViewType(offsetPosition);
        
        // 2) Find from scrap via stable ids, if exists
        if (mAdapter.hasStableIds()) {
            holder = getScrapViewForId(mAdapter.getItemId(offsetPosition), type, dryRun);
             // ... 省略代码
        }
        
        if (holder == null && mViewCacheExtension != null) {
            final View view = mViewCacheExtension.getViewForPositionAndType(this, position, type);
            if (view != null) {
                holder = getChildViewHolder(view);
                // ... 省略代码
            }
        }
        if (holder == null) { // fallback to recycler
            // ... 省略代码
            holder = getRecycledViewPool().getRecycledView(type);
            // ... 省略代码
        }
        if (holder == null) {
            holder = mAdapter.createViewHolder(RecyclerView.this, type);
            // ... 省略代码
        }
    }

    // ... 省略代码
    
    if (mState.isPreLayout() && holder.isBound()) {
        // ... 省略代码
    } else if (!holder.isBound() || holder.needsUpdate() || holder.isInvalid()) {
        // ... 省略代码
        mAdapter.bindViewHolder(holder, offsetPosition);
        // ... 省略代码
    }

    // ... 省略代码
    return holder.itemView;
}
```

这个方法比较长，我先解释下它的逻辑吧。根据列表位置获取ItemView，先后从detached scrapped、attached scrapped、cached、exCached、recycler池中查找相应的ItemView，如果没有找到，就创建（Adapter.createViewHolder()），最后与数据集绑定.

这里是Recycler中对应的缓存集合：

private ArrayList<ViewHolder> mChangedScrap         与RecyclerView分离的ViewHolder列表。
private ArrayList<ViewHolder> mAttachedScrap        未与RecyclerView分离的ViewHolder列表。
private ArrayList<ViewHolder> mCachedViews          ViewHolder缓存列表。
private ViewCacheExtension    mViewCacheExtension   开发者控制的ViewHolder缓存
private RecycledViewPool      mRecyclerPool         提供复用ViewHolder池。
public void bindViewToPosition(View view, int position)  
public View getViewForPosition(int position)

重点的部分，代码中已经给标号了，先来看看getScrapViewForPosition()方法.

```java
ViewHolder getScrapViewForPosition(int position, int type, boolean dryRun) {
    final int scrapCount = mAttachedScrap.size();

    // 在还未detach的废弃视图中查找出来一个类型匹配(无效类型)的view.
    for (int i = 0; i < scrapCount; i++) {
        final ViewHolder holder = mAttachedScrap.get(i);
        if (!holder.wasReturnedFromScrap() && holder.getLayoutPosition() == position && !holder.isInvalid() && (mState.mInPreLayout || !holder.isRemoved())) {
            if (type != INVALID_TYPE && holder.getItemViewType() != type) {
                break;
            }
            // 表明这个ViewHolder是从废弃的View集合中取出来的，可用于itemView的返回值。
            holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
            return holder;
        }
    }

    if (!dryRun) {
       // 找到已经隐藏，但是未被删除的view，然后将其detach掉，detach scrap中。
        View view = mChildHelper.findHiddenNonRemovedView(position, type);
        if (view != null) {
            final ViewHolder vh = getChildViewHolderInt(view);
            mChildHelper.unhide(view);
            int layoutIndex = mChildHelper.indexOfChild(view);
            mChildHelper.detachViewFromParent(layoutIndex);
            scrapView(view);
            vh.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP | ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST);
            return vh;
        }
    }

    // 在第一级视图缓存中查找.
    final int cacheSize = mCachedViews.size();
    for (int i = 0; i < cacheSize; i++) {
        final ViewHolder holder = mCachedViews.get(i);
        // invalid view holders may be in cache if adapter has stable ids as they can be
        // retrieved via getScrapViewForId
        if (!holder.isInvalid() && holder.getLayoutPosition() == position) {
            if (!dryRun) {
                mCachedViews.remove(i);
            }
            if (DEBUG) {
                Log.d(TAG, "getScrapViewForPosition(" + position + ", " + type +
                        ") found match in cache: " + holder);
            }
            return holder;
        }
    }
    return null;
}
```

在getScrapViewForPosition()调用的下方还有个getScrapViewForId()方法，他们都是从废弃视图中或是缓存中查找view，区别是一个根据位置，一个根据id.

需要说明一下RecycledViewPool，RecycledViewPool内部实际上是一个Map，将ItemView以ItemType分类保存了下来，这里算是RecyclerView设计上的亮点，
通过RecyclerView.RecycledViewPool可以实现在不同的RecyclerView之间共享ItemView，只要为这些不同RecyclerView设置同一个RecyclerView.RecycledViewPool就可以了。 

刚才叙述的都是查找的过程，而插入的过程位于fill()中的`recycleByLayoutState(recycler, layoutState);`中，最终实际执行到RecyclerView.Recycler.recycleViewHolderInternal()方法中。
其逻辑是，首先判断集合cached是否満了，如果已満就从cached集合中移出一个到detach scrapped集合中去，再把新的ItemView添加到cached集合；如果不満就将ItemView直接添加到cached集合。 

```java
void recycleViewHolderInternal(ViewHolder holder) {
    // 省略校验代码
    
    final boolean transientStatePreventsRecycling = holder.doesTransientStatePreventRecycling();
    final boolean forceRecycle = mAdapter != null && transientStatePreventsRecycling&& mAdapter.onFailedToRecycleView(holder);
    boolean cached = false;
    boolean recycled = false;
    
    if (forceRecycle || holder.isRecyclable()) {
        if (!holder.hasAnyOfTheFlags(ViewHolder.FLAG_INVALID | ViewHolder.FLAG_REMOVED | ViewHolder.FLAG_UPDATE)) {
            // Retire oldest cached view
            final int cachedViewSize = mCachedViews.size();
            if (cachedViewSize == mViewCacheMax && cachedViewSize > 0) {
                recycleCachedViewAt(0);
            }
            if (cachedViewSize < mViewCacheMax) {
                mCachedViews.add(holder);
                cached = true;
            }
        }
        if (!cached) {
            addViewHolderToRecycledViewPool(holder);
            recycled = true;
        }
    } else if (DEBUG) {
        Log.d(TAG, "trying to recycle a non-recycleable holder. Hopefully, it will "
                + "re-visit here. We are still removing it from animation lists");
    }
    // even if the holder is not removed, we still call this method so that it is removed
    // from view holder lists.
    mViewInfoStore.removeViewHolder(holder);
    if (!cached && !recycled && transientStatePreventsRecycling) {
        holder.mOwnerRecyclerView = null;
    }
}
```

# RecyclerView#State

包含了一些对于当前RecyclerView状态有用处的信息,例如滚动的位置or视图焦点.该状态对象还可以通过资源ID标识保持任意的数据.
很多时候,RecyclerView组件需要相互间传递信息.为了提供组件之间定义良好的数据总线,RecyclerView传递通过状态对象交互数据和进行回调。
如果要实现自己的组件，您可以使用State的PUT/GET/方法来传递您的组件之间的数据，而无需管理自己的生命周期。

# RecyclerView#Recycler

Recycler负责管理报废(Scrapped)或分离(detached)的item以便于再次利用.
一个`Scrapped`的View是依然附属(attached)于其父RecyclerView,但是已经标记为删除(removal)或重用(reuse)的视图.
一般使用Recycler场景是通过布局管理器(RecyclerView.LayoutManager)获取view,用于表示根据给定的位置(position)或是资源ID,在数据适配器中(Adapter)中获取的数据.
如果被认为重用脏(dirty)视图,适配器会要求重新绑定.如果不是,该视图可以迅速由LayoutManager进行重用.

# LinearLayout#AnchorInfo

简单的数据类，以保持锚信息(位置/坐标/锚点方向)

# RecyclerView.LayoutManager

LayoutManager的责任是负责RecyclerView的测量,item定位以及确定当item不可见时的回收策略.

# LinearLayoutManager.LayoutState

帮助LinearLayoutManager保存一些临时状态.

hasMore() 如果适配器还有更多数据则返回true

next() 获取下一个应该布局的view元素.调用RecyclerView.getViewForPosition()来得到view

# RecyclerView.ItemHolderInfo

用于保存item相关的边界信息.被用于计算item动画时.

#  关于ViewHolder

mFlags:

FLAG_BOUND——ViewHolder已经绑定到某个位置，mPosition、mItemId、mItemViewType都有效  
FLAG_UPDATE——ViewHolder绑定的View对应的数据过时需要重新绑定，mPosition、mItemId还是一致的  
FLAG_INVALID——ViewHolder绑定的View对应的数据无效，需要完全重新绑定不同的数据  
FLAG_REMOVED——ViewHolder对应的数据已经从数据集移除  
FLAG_NOT_RECYCLABLE——ViewHolder不能复用  
FLAG_RETURNED_FROM_SCRAP——这个状态的ViewHolder会加到scrap list被复用。  
FLAG_CHANGED——ViewHolder内容发生变化，通常用于表明有ItemAnimator动画  
FLAG_IGNORE——ViewHolder完全由LayoutManager管理，不能复用  
FLAG_TMP_DETACHED——ViewHolder从父RecyclerView临时分离的标志，便于后续移除或添加回来  
FLAG_ADAPTER_POSITION_UNKNOWN——ViewHolder不知道对应的Adapter的位置，直到绑定到一个新位置  
FLAG_ADAPTER_FULLUPDATE——方法addChangePayload(null)调用时设置


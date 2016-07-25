
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
            addView(view);
        } else {
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

# 滑动

# 动画

# Recycler

# 数据更新

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


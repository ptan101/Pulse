package tan.philip.nrf_ble.databinding;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityGraphBindingImpl extends ActivityGraphBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.graph1, 1);
        sViewsWithIds.put(R.id.graphLegendCL, 2);
        sViewsWithIds.put(R.id.constraintLayout, 3);
        sViewsWithIds.put(R.id.mainBannerText, 4);
        sViewsWithIds.put(R.id.deviceIDText, 5);
        sViewsWithIds.put(R.id.imageButton, 6);
        sViewsWithIds.put(R.id.digitalDisplayLeft, 7);
        sViewsWithIds.put(R.id.divider, 8);
        sViewsWithIds.put(R.id.digitalDisplayRight, 9);
        sViewsWithIds.put(R.id.divider2, 10);
        sViewsWithIds.put(R.id.digitalDisplayCenter, 11);
        sViewsWithIds.put(R.id.btn_reset, 12);
        sViewsWithIds.put(R.id.recordTimer, 13);
        sViewsWithIds.put(R.id.amplification, 14);
        sViewsWithIds.put(R.id.amplificationText, 15);
    }
    // views
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityGraphBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 16, sIncludes, sViewsWithIds));
    }
    private ActivityGraphBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.SeekBar) bindings[14]
            , (android.widget.TextView) bindings[15]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[0]
            , (android.widget.Button) bindings[12]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[3]
            , (android.widget.TextView) bindings[5]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[11]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[7]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[9]
            , (android.view.View) bindings[8]
            , (android.view.View) bindings[10]
            , (com.jjoe64.graphview.GraphView) bindings[1]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[2]
            , (android.widget.ImageButton) bindings[6]
            , (android.widget.TextView) bindings[4]
            , (android.widget.TextView) bindings[13]
            );
        this.backgroundCL.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x1L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
            return variableSet;
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        // batch finished
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): null
    flag mapping end*/
    //end
}
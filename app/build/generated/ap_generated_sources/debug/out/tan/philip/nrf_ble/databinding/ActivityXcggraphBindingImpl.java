package tan.philip.nrf_ble.databinding;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityXcggraphBindingImpl extends ActivityXcggraphBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.graph1, 1);
        sViewsWithIds.put(R.id.constraintLayout, 2);
        sViewsWithIds.put(R.id.textView4, 3);
        sViewsWithIds.put(R.id.textView5, 4);
        sViewsWithIds.put(R.id.sdDetectedText, 5);
        sViewsWithIds.put(R.id.imageButton, 6);
        sViewsWithIds.put(R.id.bottomData, 7);
        sViewsWithIds.put(R.id.HR_layout, 8);
        sViewsWithIds.put(R.id.img_HR, 9);
        sViewsWithIds.put(R.id.txt_heart_rate, 10);
        sViewsWithIds.put(R.id.PWV_layout, 11);
        sViewsWithIds.put(R.id.constraintLayout3, 12);
        sViewsWithIds.put(R.id.txtMinHR, 13);
        sViewsWithIds.put(R.id.maxHR, 14);
        sViewsWithIds.put(R.id.img_grey_bar, 15);
        sViewsWithIds.put(R.id.img_left_circle, 16);
        sViewsWithIds.put(R.id.img_right_circle, 17);
        sViewsWithIds.put(R.id.img_blue_bar, 18);
        sViewsWithIds.put(R.id.img_left_blue_circle, 19);
        sViewsWithIds.put(R.id.img_right_blue_circle, 20);
        sViewsWithIds.put(R.id.txt_low_hr, 21);
        sViewsWithIds.put(R.id.txt_high_hr, 22);
        sViewsWithIds.put(R.id.txt_single_hr, 23);
        sViewsWithIds.put(R.id.txt_warning, 24);
        sViewsWithIds.put(R.id.btn_reset, 25);
        sViewsWithIds.put(R.id.txt_scg, 26);
        sViewsWithIds.put(R.id.txt_ecg, 27);
        sViewsWithIds.put(R.id.recordTimer, 28);
        sViewsWithIds.put(R.id.amplification, 29);
        sViewsWithIds.put(R.id.amplificationText, 30);
    }
    // views
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityXcggraphBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 31, sIncludes, sViewsWithIds));
    }
    private ActivityXcggraphBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[8]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[11]
            , (android.widget.SeekBar) bindings[29]
            , (android.widget.TextView) bindings[30]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[0]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[7]
            , (android.widget.Button) bindings[25]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[2]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[12]
            , (com.jjoe64.graphview.GraphView) bindings[1]
            , (android.widget.ImageButton) bindings[6]
            , (android.widget.ImageView) bindings[18]
            , (android.widget.ImageView) bindings[15]
            , (android.widget.ImageView) bindings[9]
            , (android.widget.ImageView) bindings[19]
            , (android.widget.ImageView) bindings[16]
            , (android.widget.ImageView) bindings[20]
            , (android.widget.ImageView) bindings[17]
            , (android.widget.TextView) bindings[14]
            , (android.widget.TextView) bindings[28]
            , (android.widget.TextView) bindings[5]
            , (android.widget.TextView) bindings[3]
            , (android.widget.TextView) bindings[4]
            , (android.widget.TextView) bindings[27]
            , (android.widget.TextView) bindings[10]
            , (android.widget.TextView) bindings[22]
            , (android.widget.TextView) bindings[21]
            , (android.widget.TextView) bindings[13]
            , (android.widget.TextView) bindings[26]
            , (android.widget.TextView) bindings[23]
            , (android.widget.TextView) bindings[24]
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
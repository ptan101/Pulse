package androidx.databinding;

public class DataBinderMapperImpl extends MergedDataBinderMapper {
  DataBinderMapperImpl() {
    addMapper(new tan.philip.nrf_ble.DataBinderMapperImpl());
  }
}

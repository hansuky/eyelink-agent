package com.m2u.eyelink.collector.dao;

import com.m2u.eyelink.thrift.TSpan; 

public interface ApplicationTraceIndexDao {
    void insert(TSpan span);
}

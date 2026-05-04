package org.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.kob.backend.pojo.Record;

@Mapper
public interface RecordMapper extends BaseMapper<Record> {
}

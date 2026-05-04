package org.kob.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.kob.backend.pojo.Bot;

@Mapper
public interface BotMapper extends BaseMapper<Bot> {
}

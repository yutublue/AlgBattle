package org.kob.backend.service.impl.record;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.kob.backend.mapper.RecordMapper;
import org.kob.backend.mapper.UserMapper;
import org.kob.backend.pojo.Record;
import org.kob.backend.pojo.User;
import org.kob.backend.service.record.GetRecordListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class GetRecordListServiceImpl implements GetRecordListService {
    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public JSONObject getList(Integer page) {
        IPage<Record> recordiPage = new Page<>(page,10);
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");//将所有record按id降序排列
        List<Record> records = recordMapper.selectPage(recordiPage,queryWrapper).getRecords();

        JSONObject resp = new JSONObject();

        List<JSONObject> items =new LinkedList<>();
        for(Record record:records){
            User userA = userMapper.selectById(record.getAId());
            User userB = userMapper.selectById(record.getBId());
            JSONObject item = new JSONObject();
            item.put("a_photo",userA.getPhoto());
            item.put("a_username",userA.getUsername());
            item.put("b_photo",userB.getPhoto());
            item.put("b_username",userB.getUsername());

            String result = "平局";
            if("A".equals(record.getLoser())) result = "B胜";
            else if("B".equals(record.getLoser()))  result = "A胜";
            item.put("result",result);

            item.put("record", record);
            items.add(item);
        }

        resp.put("records",items);
        resp.put("records_count", recordMapper.selectCount(null));//返回一共有多少页(参数是null就是无条件返回)

        return resp;
    }
}

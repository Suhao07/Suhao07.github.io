package com.Auroral.blog.service.impl;

import com.Auroral.blog.dao.NoticeDao;
import com.Auroral.blog.dto.NoticeDTO;
import com.Auroral.blog.entity.Notice;
import com.Auroral.blog.service.NoticeService;
import com.Auroral.blog.utils.BeanCopyUtil;
import com.Auroral.blog.vo.NoticeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.Auroral.blog.constant.RedisPrefixConst.NOTICE_LIST_KEY;

/**
 * 公告服务实现
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeDao, Notice> implements NoticeService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<NoticeDTO> listNotices() {
        // 从数据库查询所有公告，按创建时间降序排序
        List<Notice> notices = this.list(new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getCreateTime));
        return BeanCopyUtil.copyList(notices, NoticeDTO.class);
    }

    @Override
    public List<NoticeDTO> listLatestNotices(int limit) {
        String cacheKey = NOTICE_LIST_KEY + ":" + limit;

        // 先从Redis缓存中获取
        List<NoticeDTO> latestNotices = (List<NoticeDTO>) redisTemplate.boundValueOps(cacheKey).get();
        if (latestNotices == null) {
            // 缓存中没有，从数据库查询
            List<Notice> notices = this.list(
                    new LambdaQueryWrapper<Notice>()
                            .orderByDesc(Notice::getCreateTime)
                            .last("LIMIT " + limit)
            );

            if (notices != null && !notices.isEmpty()) {
                latestNotices = BeanCopyUtil.copyList(notices, NoticeDTO.class);
            } else {
                // 如果数据库中无数据，返回一个默认公告
                latestNotices = new ArrayList<>();
                NoticeDTO defaultNotice = NoticeDTO.builder()
                        .id(0)
                        .content("发布你的第一篇公告吧")
                        .createTime(LocalDateTime.now())
                        .build();
                latestNotices.add(defaultNotice);
            }

            // 存入Redis缓存，设置1小时过期
            redisTemplate.boundValueOps(cacheKey).set(latestNotices, 1, TimeUnit.HOURS);
        }
        return latestNotices;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateNotice(NoticeVO noticeVO) {
        // VO转换为实体
        Notice notice = BeanCopyUtil.copyObject(noticeVO, Notice.class);

        // 如果是新增，设置创建时间
        if (notice.getId() == null) {
            notice.setCreateTime(LocalDateTime.now());
        }

        // 保存或更新
        this.saveOrUpdate(notice);

        // 清除Redis缓存
        redisTemplate.delete(NOTICE_LIST_KEY);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteNotice(Integer noticeId) {
        // 删除公告
        this.removeById(noticeId);

        // 清除Redis缓存
        redisTemplate.delete(NOTICE_LIST_KEY);
    }
}
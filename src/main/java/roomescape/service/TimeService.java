package roomescape.service;

import java.util.List;

import org.springframework.stereotype.Service;

import roomescape.dto.TimeResponse;
import roomescape.repository.TimeDao;

@Service
public class TimeService {
    private TimeDao timeDao;
    public TimeService(TimeDao timeDao) {
        this.timeDao = timeDao;
    }

    public List<TimeResponse> findAll() {
        return timeDao.findAll().stream()
                .map(TimeResponse::from)
                .toList();
    }
}

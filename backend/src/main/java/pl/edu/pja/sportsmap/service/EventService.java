package pl.edu.pja.sportsmap.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pl.edu.pja.sportsmap.dto.event.*;
import pl.edu.pja.sportsmap.persistence.dao.EventRepository;
import pl.edu.pja.sportsmap.persistence.dao.SportComplexRepository;
import pl.edu.pja.sportsmap.persistence.dao.UserEventRepository;
import pl.edu.pja.sportsmap.persistence.dao.UserRepository;
import pl.edu.pja.sportsmap.persistence.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SportComplexRepository sportComplexRepository;
    private final UserEventRepository userEventRepository;
    final SportComplexService sportComplexService;
    private final UserService userService;

    public EventService(EventRepository eventRepository, UserRepository userRepository, SportComplexRepository sportComplexRepository, UserEventRepository userEventRepository, SportComplexService sportComplexService, UserService userService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.sportComplexRepository = sportComplexRepository;
        this.userEventRepository = userEventRepository;
        this.sportComplexService = sportComplexService;
        this.userService = userService;
    }

    public List<Event> getAllAvailableEventsBySportComplexId(Long id) {
        LocalDateTime currentTime = LocalDateTime.now();
        return eventRepository.findAllBySportComplexIdAndEndTimeAfter(id, currentTime);
    }

    public Event addEvent(AddEventDto addEventDto) {
        Event event = convertAddEventDtoToEntity(addEventDto);
        return eventRepository.save(event);
    }

//    public Event updateEvent(Long UserId, UpdateEventDto updateEventDto){
//        Optional<Event> optionalCurrentEvent = eventRepository.findById(UserId);
//
//        if (optionalCurrentEvent.isPresent()){
//                Event currentEvent = optionalCurrentEvent.get();
//
//                currentEvent = Event.builder()
//                        .
//        }
//    }

    public JoinEventDto joinEvent(Long eventId, Long userId){
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EntityNotFoundException("Event not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found!"));

        if (userEventRepository.existsByUser_IdAndEvent_Id(user.getId(),event.getId())){
            throw new IllegalStateException("User is already joined to the event");
        }

        userEventRepository.save(new UserEvent(user,event));
        return new JoinEventDto("user " + user.getNickname() + " joined to " + event.getName() + " event.");
    }


    public Integer getInterestedPeopleCount(Long eventId) {
        return userEventRepository.countByEventId(eventId);
    }

    public Event convertAddEventDtoToEntity(AddEventDto addEventDto) {
        return Event.builder()
                .name(addEventDto.name())
                .description(addEventDto.description())
                .sportComplex(sportComplexRepository.findById(addEventDto.sportComplexId()).orElse(null))
                .startTime(addEventDto.startTime())
                .endTime(addEventDto.endTime())
                .photo(addEventDto.photo())
                .user(userRepository.findById(addEventDto.creatorId()).orElse(null))
                .build();
    }

    public GetSimpleShortEventDto convertEntityToSimpleShortEventDto(Event event) {
        return GetSimpleShortEventDto.builder()
                .photo(event.getPhoto())
                .name(event.getName())
                .sportObjectName(sportComplexService.getSportComplexNameById(event.getSportComplex().getId()))
                .startTime(event.getStartTime())
                .interested_people(userEventRepository.countByEventId(event.getId()))
                .build();
    }

    public GetSimpleDetailedEventDto convertEntityToSimpleDetailedEventDto(Event event) {
        return GetSimpleDetailedEventDto.builder()
                .name(event.getName())
                .photo(event.getPhoto())
                .sportObjectName(sportComplexService.getSportComplexNameById(event.getSportComplex().getId()))
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .interestedPeople(userEventRepository.countByEventId(event.getId()))
                .creatorNickname(userService.getNicknameById(event.getUser().getId()))
                .creatorAvatar(userService.getAvatarById(event.getUser().getId()))
                .duration(getEventDurationByEventId(event.getId()))
                .description(event.getDescription())
                .build();
    }


    public EventDurationDto getEventDurationByEventId(Long id) {
        LocalDateTime startDate = eventRepository.findById(id).orElseThrow().getStartTime();
        LocalDateTime endDate = eventRepository.findById(id).orElseThrow().getEndTime();
        Duration duration = Duration.between(startDate, endDate);
        Integer days = (int) duration.toDays();
        Integer hours = (int) duration.toHours() % 24;
        Integer minutes = (int) duration.toMinutes() % 60;

        return new EventDurationDto(days, hours, minutes);
    }

}

package com.hopesapms.app.modules.room.repository;

import com.hopesapms.app.modules.room.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByIsDeletedFalse();
}

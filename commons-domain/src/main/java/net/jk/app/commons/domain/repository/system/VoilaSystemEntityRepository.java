package net.jk.app.commons.domain.repository.system;

import net.jk.app.commons.domain.model.system.VoilaSystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoilaSystemEntityRepository extends JpaRepository<VoilaSystemEntity, Long> {}

package net.jk.app.commons.domain.model.system;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import net.jk.app.commons.boot.domain.IEntity;

@Entity
@Table(name = "v_entity")
@Data
public class VoilaSystemEntity implements IEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Override
  public String getPublicId() {
    return String.valueOf(id);
  }
}

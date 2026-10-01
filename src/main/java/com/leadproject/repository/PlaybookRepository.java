package com.leadproject.repository;

import com.leadproject.model.Playbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaybookRepository extends JpaRepository<Playbook, Long> {

	@Query("select distinct playbook from Playbook playbook left join fetch playbook.versions")
	java.util.List<Playbook> findAllWithVersions();
}

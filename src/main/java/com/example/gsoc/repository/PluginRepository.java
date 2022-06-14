package com.example.gsoc.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.gsoc.model.Plugin;

public interface PluginRepository extends JpaRepository<Plugin, Long> {

}

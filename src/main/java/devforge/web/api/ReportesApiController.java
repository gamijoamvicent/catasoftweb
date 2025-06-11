package devforge.web.api;

import devforge.config.LicoreriaContext;
import devforge.servicio.VentaCajaServicio;
import devforge.web.dto.VentaCajaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

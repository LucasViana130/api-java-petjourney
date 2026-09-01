package br.com.fiap.petjourney.models;

import br.com.fiap.petjourney.dtos.request.TutorRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TB_TUTOR")
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String cpf;

    private String phone;

    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    private Clinic clinic;

    @OneToMany(mappedBy = "tutor")
    private List<Pet> pets;

    public Tutor(TutorRequest request, Clinic clinic) {
        this.name = request.name();
        this.cpf = request.cpf();
        this.phone = request.phone();
        this.email = request.email();
        this.clinic = clinic;
    }

    public void updateFrom(TutorRequest request) {
        this.name = request.name();
        this.cpf = request.cpf();
        this.phone = request.phone();
        this.email = request.email();
    }
}

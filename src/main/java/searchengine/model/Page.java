package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "pages", indexes = @Index(columnList = "path"))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false, columnDefinition = "INT")
    private Site site;

    @Column(name = "path", columnDefinition = "VARCHAR(511)", nullable = false, length = 511)
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;


}

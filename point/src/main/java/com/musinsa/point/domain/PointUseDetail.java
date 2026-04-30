package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TB_POINT_USE_DETAIL")
@NoArgsConstructor
@Data
public class PointUseDetail {
    @Id
    @Column(name = "DETAIL_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @Column(name = "USE_ID")
    private String useId;

    @Column(name = "EARN_ID")
    private String earnId;

    @Column(name = "USE_AMOUNT")
    private Long useAmount;

    @Column(name = "CANCEL_AMOUNT")
    private Long cancelAmount;

    public PointUseDetail(String useId, String earnId, Long useAmount, Long cancelAmount) {
        this.useId = useId;
        this.earnId = earnId;
        this.useAmount = useAmount;
        this.cancelAmount = cancelAmount;
    }
}

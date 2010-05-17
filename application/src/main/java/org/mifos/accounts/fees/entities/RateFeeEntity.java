package org.mifos.accounts.fees.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.mifos.accounts.fees.business.CategoryTypeEntity;
import org.mifos.accounts.fees.business.FeeFormulaEntity;
import org.mifos.accounts.fees.exceptions.FeeException;
import org.mifos.accounts.financial.business.GLCodeEntity;
import org.mifos.customers.office.business.OfficeBO;


@Entity
@Table(name = "FEES")
@DiscriminatorValue("RATE")
public class RateFeeEntity extends FeeEntity {

    @Column(name = "RATE")
    private Double rate;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="FORMULA_ID")
    @Column(name="FORMULA_ID",insertable=true, updatable=false)
    @Access(AccessType.FIELD)
    private FeeFormulaEntity feeFormula;

    public RateFeeEntity(String feeName, CategoryTypeEntity feeCategoryType, GLCodeEntity glCode, Double rate,
            FeeFormulaEntity feeFormula, boolean isCustomerDefaultFee, OfficeBO office) throws FeeException {
        super(feeName, feeCategoryType, glCode, isCustomerDefaultFee, office);
        this.rate = rate;
        this.feeFormula = feeFormula;
    }

    protected RateFeeEntity() {
        super();
        this.feeFormula = null;
    }

    public Double getRate() {
        return this.rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public FeeFormulaEntity getFeeFormula() {
        return this.feeFormula;
    }

    public void setFeeFormula(FeeFormulaEntity feeFormula) {
        this.feeFormula = feeFormula;
    }

}

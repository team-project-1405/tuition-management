// ============================================================
// فایل: Main.java
// پروژه: سیستم مدیریت شهریه (جاوا + Spring Boot + H2)
// شامل: مدل‌ها، کنترلرها، سرویس‌ها، repositoryها و کلاس اصلی
// ============================================================

package com.tuition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("✅ سیستم مدیریت شهریه با موفقیت اجرا شد!");
        System.out.println("📌 آدرس‌ها:");
        System.out.println("   http://localhost:8080/api/students");
        System.out.println("   http://localhost:8080/api/dashboard/stats");
        System.out.println("   http://localhost:8080/h2-console");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("*");
            }
        };
    }
}

// ============================================================
// مدل‌ها (Models)
// ============================================================

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

// -------------------- Student --------------------
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(unique = true, nullable = false, length = 10)
    private String nationalCode;
    
    @Column(length = 11)
    private String phone;
    
    private String email;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Enumerated(EnumType.STRING)
    private StudentStatus status = StudentStatus.active;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date registrationDate = new Date();
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Installment> installments = new ArrayList<>();
    
    public enum StudentStatus {
        active, inactive, graduated
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

// -------------------- PaymentMethod --------------------
@Entity
@Table(name = "payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private MethodType methodType;
    
    private boolean isActive = true;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    public enum MethodType {
        cash, card, check, online
    }
}

// -------------------- Installment --------------------
@Entity
@Table(name = "installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private Double totalAmount;
    
    private Double paidAmount = 0.0;
    
    @Temporal(TemporalType.DATE)
    private Date dueDate;
    
    private boolean isPaid = false;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
    
    @OneToMany(mappedBy = "installment", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();
    
    @OneToMany(mappedBy = "installment", cascade = CascadeType.ALL)
    private List<Penalty> penalties = new ArrayList<>();
    
    public Double getRemainingAmount() {
        return totalAmount - paidAmount;
    }
    
    public boolean isOverdue() {
        return new Date().after(dueDate) && !isPaid;
    }
}

// -------------------- Payment --------------------
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "installment_id", nullable = false)
    private Installment installment;
    
    @ManyToOne
    @JoinColumn(name = "method_id")
    private PaymentMethod method;
    
    @Column(nullable = false)
    private Double amount;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date paymentDate = new Date();
    
    private String referenceNumber;
    
    private boolean isSuccessful = true;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL)
    private Receipt receipt;
}

// -------------------- Penalty --------------------
@Entity
@Table(name = "penalties")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Penalty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "installment_id", nullable = false)
    private Installment installment;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private Integer daysOverdue;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date calculatedDate = new Date();
    
    private boolean isPaid = false;
}

// -------------------- Receipt --------------------
@Entity
@Table(name = "receipts")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @Column(unique = true, nullable = false)
    private String receiptNumber;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date generatedDate = new Date();
}


// ============================================================
// Repositoryها (Repository)
// ============================================================

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

interface StudentRepository extends JpaRepository<Student, Long> {
    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "s.nationalCode LIKE CONCAT('%', :query, '%')")
    List<Student> search(@Param("query") String query);
}

interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {}

interface InstallmentRepository extends JpaRepository<Installment, Long> {
    List<Installment> findByStudentId(Long studentId);
    List<Installment> findByIsPaidFalseAndDueDateBefore(Date date);
    List<Installment> findByStudentIdAndIsPaidFalseAndDueDateBefore(Long studentId, Date date);
}

interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInstallmentStudentIdOrderByPaymentDateDesc(Long studentId);
    
    @Query("SELECT SUM(p.amount) FROM Payment p")
    Double getTotalCollected();
}

interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    List<Penalty> findByInstallmentStudentIdAndIsPaidFalse(Long studentId);
    
    @Query("SELECT SUM(p.amount) FROM Penalty p WHERE p.isPaid = false")
    Double getTotalUnpaidPenalty();
}

interface ReceiptRepository extends JpaRepository<Receipt, Long> {}


// ============================================================
// DTOها (Data Transfer Objects)
// ============================================================

import lombok.Data;

@Data
class InvoiceDTO {
    private String studentName;
    private Double totalDue;
    private Double totalPaid;
    private Double remaining;
    private Double totalPenalty;
    private List<InstallmentDTO> installments;
    
    @Data
    public static class InstallmentDTO {
        private Long id;
        private String title;
        private Double totalAmount;
        private Double paidAmount;
        private Double remaining;
        private Date dueDate;
        private boolean isPaid;
        private boolean isOverdue;
    }
}

@Data
class DashboardDTO {
    private Long totalStudents;
    private Long activeStudents;
    private Long totalInstallments;
    private Long paidInstallments;
    private Long overdueInstallments;
    private Double totalCollected;
    private Double totalPenalty;
    private Double collectionRate;
}


// ============================================================
// کنترلرها (Controllers) - Use Caseها
// ============================================================

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.stream.Collectors;

// -------------------- StudentController (UC-01 تا UC-05) --------------------
@RestController
@RequestMapping("/api/students")
class StudentController {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @PostMapping
    @Operation(summary = "UC-01: ثبت دانش‌آموز جدید")
    public ResponseEntity<Student> registerStudent(@RequestBody Student student) {
        student.setRegistrationDate(new Date());
        student.setUpdatedAt(new Date());
        return ResponseEntity.ok(studentRepository.save(student));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "UC-02: ویرایش اطلاعات")
    public ResponseEntity<Student> editStudent(@PathVariable Long id, @RequestBody Student student) {
        Student existing = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("دانش‌آموز یافت نشد"));
        student.setId(id);
        student.setUpdatedAt(new Date());
        return ResponseEntity.ok(studentRepository.save(student));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "UC-03: حذف دانش‌آموز")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    @Operation(summary = "UC-04: جستجوی دانش‌آموز")
    public ResponseEntity<List<Student>> searchStudents(@RequestParam String q) {
        return ResponseEntity.ok(studentRepository.search(q));
    }
    
    @PatchMapping("/{id}/status")
    @Operation(summary = "UC-05: تغییر وضعیت")
    public ResponseEntity<Student> changeStatus(@PathVariable Long id, @RequestParam Student.StudentStatus status) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("دانش‌آموز یافت نشد"));
        student.setStatus(status);
        student.setUpdatedAt(new Date());
        return ResponseEntity.ok(studentRepository.save(student));
    }
    
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }
}

// -------------------- PaymentMethodController (UC-06) --------------------
@RestController
@RequestMapping("/api/payment-methods")
class PaymentMethodController {
    
    @Autowired
    private PaymentMethodRepository repository;
    
    @PostMapping
    @Operation(summary = "UC-06: تعریف روش پرداخت")
    public PaymentMethod create(@RequestBody PaymentMethod method) {
        return repository.save(method);
    }
    
    @GetMapping
    public List<PaymentMethod> getAll() {
        return repository.findAll();
    }
}

// -------------------- InstallmentController (UC-07) --------------------
@RestController
@RequestMapping("/api/installments")
class InstallmentController {
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @PostMapping
    @Operation(summary = "UC-07: تعریف قسط")
    public Installment create(@RequestBody Installment installment) {
        installment.setCreatedAt(new Date());
        return installmentRepository.save(installment);
    }
    
    @GetMapping("/student/{studentId}")
    public List<Installment> getByStudent(@PathVariable Long studentId) {
        return installmentRepository.findByStudentId(studentId);
    }
}

// -------------------- PaymentController (UC-08 و UC-09) --------------------
@RestController
@RequestMapping("/api/payments")
class PaymentController {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private ReceiptRepository receiptRepository;
    
    @PostMapping
    @Operation(summary = "UC-08: ثبت پرداخت + UC-09: تولید رسید")
    public Payment createPayment(@RequestBody Payment payment) {
        Installment installment = installmentRepository.findById(payment.getInstallment().getId())
            .orElseThrow(() -> new RuntimeException("قسط یافت نشد"));
        
        installment.setPaidAmount(installment.getPaidAmount() + payment.getAmount());
        if (installment.getPaidAmount() >= installment.getTotalAmount()) {
            installment.setPaid(true);
        }
        installmentRepository.save(installment);
        
        Payment savedPayment = paymentRepository.save(payment);
        Receipt receipt = new Receipt();
        receipt.setPayment(savedPayment);
        receipt.setReceiptNumber("RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        receipt.setGeneratedDate(new Date());
        receiptRepository.save(receipt);
        
        return savedPayment;
    }
}

// -------------------- PenaltyController (UC-10) --------------------
@RestController
@RequestMapping("/api/penalties")
class PenaltyController {
    
    @Autowired
    private PenaltyRepository penaltyRepository;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @PostMapping("/calculate")
    @Operation(summary = "UC-10: محاسبه جریمه")
    public List<Penalty> calculateAll() {
        List<Penalty> createdPenalties = new ArrayList<>();
        Date now = new Date();
        
        List<Installment> overdue = installmentRepository.findByIsPaidFalseAndDueDateBefore(now);
        
        for (Installment installment : overdue) {
            long diffMs = now.getTime() - installment.getDueDate().getTime();
            int daysOverdue = (int) java.util.concurrent.TimeUnit.DAYS.convert(diffMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            double dailyRate = 0.001;
            double penaltyAmount = installment.getTotalAmount() * dailyRate * daysOverdue;
            
            Penalty penalty = new Penalty();
            penalty.setInstallment(installment);
            penalty.setAmount(penaltyAmount);
            penalty.setDaysOverdue(daysOverdue);
            penalty.setCalculatedDate(now);
            
            createdPenalties.add(penaltyRepository.save(penalty));
        }
        
        return createdPenalties;
    }
}

// -------------------- InvoiceController (UC-11) --------------------
@RestController
@RequestMapping("/api/invoices")
class InvoiceController {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private PenaltyRepository penaltyRepository;
    
    @GetMapping("/student/{studentId}")
    @Operation(summary = "UC-11: صورت‌حساب")
    public InvoiceDTO getInvoice(@PathVariable Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("دانش‌آموز یافت نشد"));
        
        List<Installment> installments = installmentRepository.findByStudentId(studentId);
        
        InvoiceDTO dto = new InvoiceDTO();
        dto.setStudentName(student.getFullName());
        dto.setTotalDue(installments.stream().mapToDouble(Installment::getTotalAmount).sum());
        dto.setTotalPaid(installments.stream().mapToDouble(Installment::getPaidAmount).sum());
        dto.setRemaining(dto.getTotalDue() - dto.getTotalPaid());
        
        double totalPenalty = penaltyRepository.findByInstallmentStudentIdAndIsPaidFalse(studentId)
            .stream().mapToDouble(Penalty::getAmount).sum();
        dto.setTotalPenalty(totalPenalty);
        
        List<InvoiceDTO.InstallmentDTO> dtos = installments.stream().map(i -> {
            InvoiceDTO.InstallmentDTO idto = new InvoiceDTO.InstallmentDTO();
            idto.setId(i.getId());
            idto.setTitle(i.getTitle());
            idto.setTotalAmount(i.getTotalAmount());
            idto.setPaidAmount(i.getPaidAmount());
            idto.setRemaining(i.getRemainingAmount());
            idto.setDueDate(i.getDueDate());
            idto.setPaid(i.isPaid());
            idto.setOverdue(i.isOverdue());
            return idto;
        }).collect(Collectors.toList());
        
        dto.setInstallments(dtos);
        return dto;
    }
}

// -------------------- TransactionController (UC-12) --------------------
@RestController
@RequestMapping("/api/transactions")
class TransactionController {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @GetMapping
    @Operation(summary = "UC-12: تراکنش‌ها")
    public List<Payment> getAllTransactions() {
        return paymentRepository.findAll();
    }
    
    @GetMapping("/student/{studentId}")
    public List<Payment> getStudentTransactions(@PathVariable Long studentId) {
        return paymentRepository.findByInstallmentStudentIdOrderByPaymentDateDesc(studentId);
    }
}

// -------------------- DebtorController (UC-13) --------------------
@RestController
@RequestMapping("/api/debtors")
class DebtorController {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private PenaltyRepository penaltyRepository;
    
    @GetMapping
    @Operation(summary = "UC-13: لیست بدهکاران")
    public List<Map<String, Object>> getDebtors() {
        Date now = new Date();
        List<Student> debtors = studentRepository.findAll().stream()
            .filter(s -> !installmentRepository.findByStudentIdAndIsPaidFalseAndDueDateBefore(s.getId(), now).isEmpty())
            .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student student : debtors) {
            List<Installment> overdue = installmentRepository.findByStudentIdAndIsPaidFalseAndDueDateBefore(student.getId(), now);
            double totalDebt = overdue.stream().mapToDouble(Installment::getRemainingAmount).sum();
            double totalPenalty = penaltyRepository.findByInstallmentStudentIdAndIsPaidFalse(student.getId())
                .stream().mapToDouble(Penalty::getAmount).sum();
            
            Map<String, Object> item = new HashMap<>();
            item.put("student", student);
            item.put("totalDebt", totalDebt);
            item.put("totalPenalty", totalPenalty);
            item.put("overdueCount", overdue.size());
            result.add(item);
        }
        return result;
    }
}

// -------------------- ReportController (UC-14) --------------------
@RestController
@RequestMapping("/api/reports")
class ReportController {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @GetMapping("/financial")
    @Operation(summary = "UC-14: خروجی گزارش")
    public Map<String, Object> getFinancialReport(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        List<Payment> payments = paymentRepository.findAll();
        
        if (startDate != null) {
            payments = payments.stream()
                .filter(p -> !p.getPaymentDate().before(startDate))
                .collect(Collectors.toList());
        }
        if (endDate != null) {
            payments = payments.stream()
                .filter(p -> !p.getPaymentDate().after(endDate))
                .collect(Collectors.toList());
        }
        
        double totalCollected = payments.stream().mapToDouble(Payment::getAmount).sum();
        
        Map<String, Object> result = new HashMap<>();
        result.put("period", (startDate != null ? startDate : "ابتدا") + " تا " + (endDate != null ? endDate : "اکنون"));
        result.put("totalCollected", totalCollected);
        result.put("transactionsCount", payments.size());
        result.put("payments", payments);
        return result;
    }
}

// -------------------- DashboardController (UC-15) --------------------
@RestController
@RequestMapping("/api/dashboard")
class DashboardController {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PenaltyRepository penaltyRepository;
    
    @GetMapping("/stats")
    @Operation(summary = "UC-15: داشبورد")
    public DashboardDTO getStats() {
        Date now = new Date();
        List<Student> students = studentRepository.findAll();
        List<Installment> installments = installmentRepository.findAll();
        
        DashboardDTO dto = new DashboardDTO();
        dto.setTotalStudents((long) students.size());
        dto.setActiveStudents(students.stream()
            .filter(s -> s.getStatus() == Student.StudentStatus.active).count());
        dto.setTotalInstallments((long) installments.size());
        dto.setPaidInstallments(installments.stream().filter(Installment::isPaid).count());
        dto.setOverdueInstallments(installments.stream()
            .filter(i -> !i.isPaid() && i.getDueDate().before(now)).count());
        dto.setTotalCollected(paymentRepository.getTotalCollected() != null ? 
            paymentRepository.getTotalCollected() : 0.0);
        dto.setTotalPenalty(penaltyRepository.getTotalUnpaidPenalty() != null ? 
            penaltyRepository.getTotalUnpaidPenalty() : 0.0);
        
        double total = dto.getTotalInstallments();
        double paid = dto.getPaidInstallments();
        dto.setCollectionRate(total > 0 ? Math.round((paid / total) * 10000.0) / 100.0 : 0.0);
        
        return dto;
    }
}

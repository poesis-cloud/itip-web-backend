package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.repository.PolicyRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

  @Mock private PolicyRepository policyRepository;

  @InjectMocks private PolicyService policyService;

  @Test
  @SuppressWarnings("null")
  void requireAllShouldReturnPoliciesForResolvedIds() {
    UUID firstPolicyId = UUID.randomUUID();
    UUID secondPolicyId = UUID.randomUUID();
    Set<UUID> policyIds = Set.of(firstPolicyId, secondPolicyId);
    Policy firstPolicy = Policy.builder().id(firstPolicyId).build();
    Policy secondPolicy = Policy.builder().id(secondPolicyId).build();
    when(policyRepository.findAllById(any())).thenReturn(List.of(secondPolicy, firstPolicy));

    Set<Policy> resolvedPolicies = policyService.requireAll(policyIds);

    assertThat(resolvedPolicies).containsExactly(secondPolicy, firstPolicy);
    verify(policyRepository).findAllById(policyIds);
  }

  @Test
  @SuppressWarnings("null")
  void requireAllShouldReturnEmptySetForEmptyIds() {
    when(policyRepository.findAllById(Set.of())).thenReturn(List.of());

    assertThat(policyService.requireAll(Set.of())).isEmpty();
    verify(policyRepository).findAllById(Set.of());
  }

  @Test
  @SuppressWarnings("null")
  void requireAllShouldTreatNullIdsAsEmpty() {
    when(policyRepository.findAllById(Set.of())).thenReturn(List.of());

    assertThat(policyService.requireAll(null)).isEmpty();
    verify(policyRepository).findAllById(Set.of());
  }

  @Test
  @SuppressWarnings("null")
  void requireAllShouldThrowWhenAnyRequestedPolicyIsMissing() {
    UUID foundPolicyId = UUID.randomUUID();
    UUID missingPolicyId = UUID.randomUUID();
    Policy foundPolicy = Policy.builder().id(foundPolicyId).build();
    when(policyRepository.findAllById(Set.of(foundPolicyId, missingPolicyId)))
        .thenReturn(List.of(foundPolicy));

    assertThatThrownBy(() -> policyService.requireAll(Set.of(foundPolicyId, missingPolicyId)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("One or more policies do not exist");
  }

  @Test
  @SuppressWarnings("null")
  void requireAllShouldThrowWhenRepositoryReturnsNullPolicy() {
    UUID policyId = UUID.randomUUID();
    when(policyRepository.findAllById(Set.of(policyId)))
        .thenReturn(java.util.Collections.singletonList(null));

    assertThatThrownBy(() -> policyService.requireAll(Set.of(policyId)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Policies must not be null");
  }
}

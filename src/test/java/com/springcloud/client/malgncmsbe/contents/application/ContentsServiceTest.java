package com.springcloud.client.malgncmsbe.contents.application;

import com.springcloud.client.malgncmsbe.common.exception.BusinessException;
import com.springcloud.client.malgncmsbe.common.exception.ErrorCode;
import com.springcloud.client.malgncmsbe.contents.application.command.CreateContentsCommand;
import com.springcloud.client.malgncmsbe.contents.application.command.UpdateContentsCommand;
import com.springcloud.client.malgncmsbe.contents.application.result.ContentsResult;
import com.springcloud.client.malgncmsbe.contents.domain.Contents;
import com.springcloud.client.malgncmsbe.contents.domain.ContentsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentsServiceTest {

    @InjectMocks
    private ContentsService contentsService;

    @Mock
    private ContentsRepository contentsRepository;

    @Test
    @DisplayName("콘텐츠를 생성하면 저장된 결과를 반환한다")
    void create_shouldReturnSavedContents() {
        Contents saved = Contents.builder().title("제목").description("내용").build();
        given(contentsRepository.save(any(Contents.class))).willReturn(saved);

        ContentsResult result = contentsService.create(new CreateContentsCommand("제목", "내용"));

        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.description()).isEqualTo("내용");
    }

    @Test
    @DisplayName("상세 조회 시 조회수 증가 쿼리가 실행된다")
    void getContentsDetail_shouldCallIncrementViewCount() {
        Contents contents = Contents.builder().title("제목").description("내용").build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));

        contentsService.getContentsDetail(1L);

        verify(contentsRepository).incrementViewCount(1L);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 조회 시 CONTENTS_NOT_FOUND 예외를 던진다")
    void getContentsDetail_shouldThrowException_whenNotFound() {
        given(contentsRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contentsService.getContentsDetail(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTENTS_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자 본인이 수정하면 성공한다")
    void update_shouldSucceed_whenOwner() {
        Contents contents = Contents.builder().title("원제목").description("원내용").build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));
        // createdBy는 Auditing으로 설정되므로 리플렉션으로 주입
        setField(contents, "createdBy", "user1");

        ContentsResult result = contentsService.update(
                new UpdateContentsCommand(1L, "새제목", "새내용"), "user1", "USER");

        assertThat(result.title()).isEqualTo("새제목");
    }

    @Test
    @DisplayName("ADMIN은 타인 콘텐츠도 수정할 수 있다")
    void update_shouldSucceed_whenAdmin() {
        Contents contents = Contents.builder().title("원제목").description("원내용").build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));
        setField(contents, "createdBy", "user1");

        ContentsResult result = contentsService.update(
                new UpdateContentsCommand(1L, "새제목", "새내용"), "admin", "ADMIN");

        assertThat(result.title()).isEqualTo("새제목");
    }

    @Test
    @DisplayName("타인 콘텐츠를 수정하려 하면 FORBIDDEN 예외를 던진다")
    void update_shouldThrowForbidden_whenNotOwnerAndNotAdmin() {
        Contents contents = Contents.builder().title("원제목").description("원내용").build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));
        setField(contents, "createdBy", "user1");

        assertThatThrownBy(() -> contentsService.update(
                new UpdateContentsCommand(1L, "새제목", "새내용"), "user2", "USER"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("작성자 본인이 삭제하면 성공한다")
    void delete_shouldSucceed_whenOwner() {
        Contents contents = Contents.builder().title("제목").description("내용").build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));
        setField(contents, "createdBy", "user1");

        contentsService.delete(1L, "user1", "USER");

        verify(contentsRepository).delete(contents);
    }

    @Test
    @DisplayName("타인 콘텐츠를 삭제하려 하면 FORBIDDEN 예외를 던진다")
    void delete_shouldThrowForbidden_whenNotOwnerAndNotAdmin() {
        Contents contents = Contents.builder().title("제목").description("내용").build();
        given(contentsRepository.findById(1L)).willReturn(Optional.of(contents));
        setField(contents, "createdBy", "user1");

        assertThatThrownBy(() -> contentsService.delete(1L, "user2", "USER"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

<template>
  <div class="">
    <div class="bg-white d-flex flex-column items-center mt-15">
      <div class="d-flex flex-column" style="width: 1300px">
        <div class="text-gray-500 point-font">
          <span class="tossface text-xl">📝</span> 공부하마 노트 작성
        </div>
        <input
          v-model="title"
          variant="plain"
          placeholder="어떤 주제에 대해 공부하셨나요?"
          class="note-title"
        />
        <textarea
          v-model="content"
          variant="plain"
          placeholder="공부한 내용을 작성해주세요. ( •̀ ω •́ )✧"
          class="note-content"
          rows="20"
        ></textarea>
      </div>
      <div class="d-flex justify-end mt-10" style="width: 1300px">
        <v-btn
          @click="CreateNote"
          size="x-large"
          class="save"
          variant="flat"
          color="#3fb1fa"
          rounded="xl"
          ><div class="save-btn">저장</div></v-btn
        >
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import instance from '@/api/index'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'

const router = useRouter()
const title = ref('')
const content = ref('')

const studyList = ref([])

function CreateNote() {
  if (title.value === '') {
    Swal.fire({
      title: '작성한 내용이 없어요',
      text: '제목은 필수 내용은 선택!',
      icon: 'question'
    })
  } else {
    Swal.fire({
      title: '노트를 저장하시겠습니까?',
      showCancelButton: true,
      confirmButtonText: '저장하기'
    }).then((result) => {
      if (result.isConfirmed) {
        instance
          .post(`api/notes`, {
            title: title.value,
            content: content.value
          })
          .then((res) => {
            if (res.data.status == 201) {
              Swal.fire('저장되었습니다!', '', 'success')
              console.log(res)
              const noteId = res.data.data.noteId
              router.push({ name: 'note', params: { id: noteId } })
            } else {
              console.log(res)
            }
          })
          .catch((err) => {
            const noteId = 1
            router.push({ name: 'note', params: { id: noteId } })
            Swal.fire('저장에 실패했어요<br>잠시 후 다시 시도해주세요', '', 'info')
            // alert('저장에 실패했어요 잠시 후 다시 시도해주세요')
            console.log('저장실패', err)
            router.push({ name: 'note', params: { id: noteId } })
          })
      }
    })
  }
}
</script>

<style scoped>
.note-title {
  font-size: x-large;
  outline: none;
  margin: 20px 0px;
}

.note-content {
  font-size: large;
  outline: none;
  /* line-height: 30px; */
}
</style>

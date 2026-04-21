const texts = [
                "Full-Stack Java Developer",
                "Web Designer",
                "Frontend Developer"
            ];

            let index = 0;
            let charIndex = 0;
            const speed = 100;
            const eraseSpeed = 50;
            const delay = 1500;

            const typeElement = document.getElementById("typewriter");
            if (typeElement) {
                typeElement.textContent = "";
            }

            function type() {
                if (charIndex < texts[index].length) {
                    typeElement.textContent += texts[index].charAt(charIndex);
                    charIndex++;
                    setTimeout(type, speed);
                } else {
                    setTimeout(erase, delay);
                }
            }

            function erase() {
                if (charIndex > 0) {
                    typeElement.textContent =
                        texts[index].substring(0, charIndex - 1);
                    charIndex--;
                    setTimeout(erase, eraseSpeed);
                } else {
                    index = (index + 1) % texts.length;
                    setTimeout(type, speed);
                }
            }

            document.addEventListener("DOMContentLoaded", type);

            // Projects section
           
function openModal(title) {
  document.getElementById("projectModal").style.display = "flex";
  document.getElementById("modalTitle").innerText = title;
}

function closeModal() {
  document.getElementById("projectModal").style.display = "none";
}
// skills section


// education section


  const timelineItems = document.querySelectorAll(".timeline li");

  function showTimelineItems() {
    const triggerBottom = window.innerHeight * 0.85;

    timelineItems.forEach(item => {
      const boxTop = item.getBoundingClientRect().top;

      if (boxTop < triggerBottom) {
        item.classList.add("show");
      }
    });
  }

  window.addEventListener("scroll", showTimelineItems);
  showTimelineItems(); // run on load



